package com.example.healthexam.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.healthexam.common.BizException;
import com.example.healthexam.common.PageResult;
import com.example.healthexam.entity.*;
import com.example.healthexam.mapper.*;
import com.example.healthexam.security.UserContext;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * 创新点：体检时段拥挤度推荐。
 * 设计要点（对应需求文档「创新点设计」）：
 * 1) 六项权重可在规则配置页调整，合计必须 100%，每次保存生成新版本并记录生效时间；
 * 2) 每次计算把输入快照、规则版本、各项得分、总分与等级落库，结果可追溯、可复算；
 * 3) 结果页给出「推荐结果 + 主要影响因素 + 未满足条件 + 可改进项」，不是只给一个分数；
 * 4) 业务负责人可人工调整最终结果，必须填原因，原始计算结果不被覆盖（另存 adjusted_score）；
 * 5) 看板统计采纳率、人工调整率。
 */
@Service
@RequiredArgsConstructor
public class CrowdService {

    private final CrowdRuleMapper ruleMapper;
    private final CrowdScoreMapper scoreMapper;
    private final AppointmentSlotMapper slotMapper;
    private final ExamAppointmentMapper appMapper;
    private final ExamPackageMapper packageMapper;
    private final ExamPackageItemMapper packageItemMapper;
    private final ExamProgressMapper progressMapper;

    private final ObjectMapper json = new ObjectMapper();

    /** 六项因素：key → 中文名，顺序固定，前后端一致 */
    public static final LinkedHashMap<String, String> FACTORS = new LinkedHashMap<>();
    static {
        FACTORS.put("deptCapacity", "科室容量");
        FACTORS.put("itemMix", "项目组合");
        FACTORS.put("slotLoad", "时段预约量");
        FACTORS.put("duration", "预计时长");
        FACTORS.put("arrivalRate", "历史到检率");
        FACTORS.put("deviceDown", "设备停用情况");
    }

    public CrowdRule activeRule() {
        CrowdRule rule = ruleMapper.selectOne(new LambdaQueryWrapper<CrowdRule>()
                .eq(CrowdRule::getStatus, "ACTIVE").orderByDesc(CrowdRule::getVersionNo).last("limit 1"));
        if (rule == null) {
            throw new BizException("尚未配置拥挤度规则，请先在规则配置页保存一版");
        }
        return rule;
    }

    public List<CrowdRule> rules() {
        return ruleMapper.selectList(new LambdaQueryWrapper<CrowdRule>().orderByDesc(CrowdRule::getVersionNo));
    }

    /** 保存新规则版本：权重合计必须 100，旧版本置为 HISTORY */
    @Transactional
    public CrowdRule saveRule(Map<String, Integer> weights, Map<String, Integer> thresholds) {
        int sum = weights.values().stream().mapToInt(Integer::intValue).sum();
        if (sum != 100) {
            throw new BizException("权重合计必须等于 100%，当前为 " + sum + "%");
        }
        for (String k : FACTORS.keySet()) {
            Integer v = weights.get(k);
            if (v == null || v < 0 || v > 100) {
                throw new BizException("权重项「" + FACTORS.get(k) + "」取值不合法");
            }
        }
        Integer maxVer = ruleMapper.selectList(new LambdaQueryWrapper<CrowdRule>()
                        .orderByDesc(CrowdRule::getVersionNo).last("limit 1"))
                .stream().findFirst().map(CrowdRule::getVersionNo).orElse(0);
        ruleMapper.update(null, new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<CrowdRule>()
                .eq(CrowdRule::getStatus, "ACTIVE").set(CrowdRule::getStatus, "HISTORY"));
        CrowdRule r = new CrowdRule();
        r.setVersionNo(maxVer + 1);
        try {
            r.setWeightsJson(json.writeValueAsString(weights));
            r.setThresholdsJson(json.writeValueAsString(
                    thresholds == null ? Map.of("recommend", 40, "normal", 70) : thresholds));
        } catch (Exception e) {
            throw new BizException("规则序列化失败");
        }
        r.setStatus("ACTIVE");
        r.setEffectiveTime(LocalDateTime.now());
        r.setCreateBy(UserContext.userId());
        r.setRemark("权重合计 100%，保存后仅影响新计算与主动重算的数据");
        r.setCreateTime(LocalDateTime.now());
        ruleMapper.insert(r);
        return r;
    }

    /**
     * 执行一次拥挤度计算。
     * @param persist true 时落库留痕；false 用于「预览计算」不落库
     */
    @Transactional
    public CrowdScore evaluate(Long slotId, Long appId, boolean persist) {
        AppointmentSlot slot = slotMapper.selectById(slotId);
        if (slot == null) {
            throw new BizException("时段不存在");
        }
        CrowdRule rule = activeRule();
        Map<String, Integer> weights = readWeights(rule);
        Map<String, Integer> th = readThresholds(rule);

        // ---- 输入因素：全部来自库内真实数据，缺失项记入 unmet 并从计算中剔除 ----
        Map<String, Double> values = new LinkedHashMap<>();
        List<String> missing = new ArrayList<>();

        int quota = nz(slot.getPersonalQuota()) + nz(slot.getGroupQuota());
        int booked = nz(slot.getPersonalBooked()) + nz(slot.getGroupBooked());
        if (quota > 0) {
            values.put("slotLoad", clamp(booked * 1.0 / quota));
        } else {
            missing.add("时段预约量");
        }
        if (nz(slot.getDeptCapacity()) > 0) {
            values.put("deptCapacity", clamp(booked * 1.0 / slot.getDeptCapacity()));
        } else {
            missing.add("科室容量");
        }

        ExamAppointment app = appId == null ? null : appMapper.selectById(appId);
        if (app != null) {
            ExamPackage pkg = packageMapper.selectById(app.getPackageId());
            if (pkg != null) {
                values.put("duration", clamp(nz(pkg.getDurationMin()) / 180.0));
                Long links = packageItemMapper.selectCount(new LambdaQueryWrapper<ExamPackageItem>()
                        .eq(ExamPackageItem::getPackageId, pkg.getId()));
                long depts = packageItemMapper.selectList(new LambdaQueryWrapper<ExamPackageItem>()
                                .eq(ExamPackageItem::getPackageId, pkg.getId())).stream()
                        .map(l -> l.getItemId()).distinct().count();
                // 项目组合复杂度：项目数占 28 项为满，跨科室数作为加成
                values.put("itemMix", clamp(links.intValue() * 1.0 / 28 + Math.min(depts, 8) * 0.02));
            }
        } else {
            missing.add("项目组合");
        }

        // 历史到检率：该时段近 90 天「已到检及以上」占比
        LocalDateTime since = LocalDateTime.now().minusDays(90);
        Long totalHist = appMapper.selectCount(new LambdaQueryWrapper<ExamAppointment>()
                .eq(ExamAppointment::getSlotId, slotId).ge(ExamAppointment::getCreateTime, since));
        Long arrived = appMapper.selectCount(new LambdaQueryWrapper<ExamAppointment>()
                .eq(ExamAppointment::getSlotId, slotId).ge(ExamAppointment::getCreateTime, since)
                .in(ExamAppointment::getStatus, List.of("CHECKING", "WAIT_REPORT", "WAIT_PICKUP", "COMPLETED")));
        if (totalHist != null && totalHist > 0) {
            values.put("arrivalRate", clamp(arrived * 1.0 / totalHist));
        } else {
            // 无历史数据时按行业经验值 0.7 兜底，并明确提示这是估算而非实测
            values.put("arrivalRate", 0.7);
            missing.add("历史到检率（按经验值 0.7 估算）");
        }
        // 设备停用：当日该项目相关进度里出现补检视为设备异常
        Long supplements = progressMapper.selectCount(new LambdaQueryWrapper<ExamProgress>()
                .eq(ExamProgress::getStatus, "SUPPLEMENT").ge(ExamProgress::getUpdateTime, LocalDateTime.now().minusDays(7)));
        values.put("deviceDown", clamp(supplements.intValue() * 1.0 / 20));

        // ---- 加权计算：缺失项不计入分母，保证分数可比 ----
        double weighted = 0;
        double weightSum = 0;
        List<Map<String, Object>> detail = new ArrayList<>();
        for (String k : FACTORS.keySet()) {
            int w = weights.getOrDefault(k, 0);
            Double v = values.get(k);
            if (v == null) {
                detail.add(Map.of("key", k, "name", FACTORS.get(k), "weight", w,
                        "value", 0, "contribution", 0, "missing", true));
                continue;
            }
            double contribution = v * 100 * w / 100.0;
            weighted += contribution;
            weightSum += w;
            detail.add(Map.of("key", k, "name", FACTORS.get(k), "weight", w,
                    "value", round(v * 100, 1), "contribution", round(contribution, 2), "missing", false));
        }
        double total = weightSum == 0 ? 0 : weighted * 100.0 / weightSum;
        total = round(Math.min(100, total), 1);
        String level = total <= th.getOrDefault("recommend", 40) ? "RECOMMEND"
                : (total <= th.getOrDefault("normal", 70) ? "NORMAL" : "NOT_RECOMMEND");

        // 主要影响因素：贡献值最高的两项
        List<Map<String, Object>> sorted = new ArrayList<>(detail);
        sorted.removeIf(d -> Boolean.TRUE.equals(d.get("missing")));
        sorted.sort((a, b) -> Double.compare((double) b.get("contribution"), (double) a.get("contribution")));
        String main = sorted.isEmpty() ? "无可用数据" : sorted.stream().limit(2)
                .map(d -> d.get("name") + " " + d.get("value") + "（权重 " + d.get("weight") + "）")
                .reduce((x, y) -> x + "、" + y).orElse("无可用数据");

        String unmet = missing.isEmpty() ? "无" : String.join("；", missing);
        String tip;
        if ("RECOMMEND".equals(level)) {
            tip = "该时段负载较低，可直接预约；建议提前 10 分钟到检以错开高峰。";
        } else if ("NORMAL".equals(level)) {
            tip = "该时段较拥挤，建议改选同日更早的时段，或提前预约空腹项目。";
        } else {
            tip = "该时段接近满额，建议改选其他日期；如必须当日体检，请预留 30 分钟以上等待时间。";
        }

        if (!persist) {
            CrowdScore preview = new CrowdScore();
            preview.setTotalScore(BigDecimal.valueOf(total));
            preview.setLevel(level);
            preview.setMainFactors(main);
            preview.setUnmetConditions(unmet);
            preview.setImproveTip(tip);
            preview.setRuleVersion(rule.getVersionNo());
            return preview;
        }

        CrowdScore s = new CrowdScore();
        s.setScoreNo("CS-" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd")) + "-"
                + String.format("%04d", (int) (System.currentTimeMillis() % 9000) + 1000));
        s.setAppId(appId);
        s.setSlotId(slotId);
        s.setRuleId(rule.getId());
        s.setRuleVersion(rule.getVersionNo());
        try {
            Map<String, Object> snapshot = new LinkedHashMap<>();
            snapshot.put("ruleVersion", "v" + rule.getVersionNo());
            snapshot.put("slotId", slotId);
            snapshot.put("appointmentId", appId);
            snapshot.put("inputs", values);
            snapshot.put("weights", weights);
            snapshot.put("thresholds", th);
            s.setInputSnapshot(json.writerWithDefaultPrettyPrinter().writeValueAsString(snapshot));
            s.setDetailJson(json.writeValueAsString(detail));
        } catch (Exception e) {
            s.setInputSnapshot("{}");
            s.setDetailJson("[]");
        }
        s.setTotalScore(BigDecimal.valueOf(total));
        s.setLevel(level);
        s.setMainFactors(main);
        s.setUnmetConditions(unmet);
        s.setImproveTip(tip);
        s.setCreateTime(LocalDateTime.now());
        scoreMapper.insert(s);
        return s;
    }

    /** 人工调整：必须填原因，原分数不动，只写 adjusted_score */
    @Transactional
    public void adjust(Long scoreId, Double adjusted, String reason) {
        if (reason == null || reason.trim().length() < 5) {
            throw new BizException("人工调整必须填写原因，且不少于 5 个字");
        }
        if (adjusted == null || adjusted < 0 || adjusted > 100) {
            throw new BizException("调整后的分数应在 0-100 之间");
        }
        CrowdScore s = scoreMapper.selectById(scoreId);
        if (s == null) {
            throw new BizException("计算记录不存在");
        }
        s.setAdjustedScore(BigDecimal.valueOf(adjusted));
        s.setAdjustReason(reason.trim());
        s.setAdjustBy(UserContext.userId());
        s.setAdjustTime(LocalDateTime.now());
        scoreMapper.updateById(s);
    }

    public PageResult<CrowdScore> page(long pageNum, long pageSize, String level, String scoreNo) {
        LambdaQueryWrapper<CrowdScore> w = new LambdaQueryWrapper<>();
        if (level != null && !level.isBlank() && !"ALL".equals(level)) {
            w.eq(CrowdScore::getLevel, level);
        }
        if (scoreNo != null && !scoreNo.isBlank()) {
            w.like(CrowdScore::getScoreNo, scoreNo);
        }
        w.orderByDesc(CrowdScore::getCreateTime);
        return PageResult.of(scoreMapper.selectPage(new Page<>(pageNum, pageSize), w));
    }

    public CrowdScore latestOfApp(Long appId) {
        if (appId == null) {
            return null;
        }
        return scoreMapper.selectOne(new LambdaQueryWrapper<CrowdScore>()
                .eq(CrowdScore::getAppId, appId).orderByDesc(CrowdScore::getId).last("limit 1"));
    }

    /** 采纳率 / 人工调整率：推荐等级的单中未被调整的比例 */
    public Map<String, Object> effect() {
        Long total = scoreMapper.selectCount(new LambdaQueryWrapper<>());
        Long recommended = scoreMapper.selectCount(new LambdaQueryWrapper<CrowdScore>()
                .eq(CrowdScore::getLevel, "RECOMMEND"));
        Long adjusted = scoreMapper.selectCount(new LambdaQueryWrapper<CrowdScore>()
                .isNotNull(CrowdScore::getAdjustedScore));
        double adoptRate = total == 0 ? 0 : round((total - adjusted) * 100.0 / total, 1);
        double adjustRate = total == 0 ? 0 : round(adjusted * 100.0 / total, 1);
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("total", total);
        m.put("recommended", recommended);
        m.put("adjusted", adjusted);
        m.put("adoptRate", adoptRate);
        m.put("adjustRate", adjustRate);
        return m;
    }

    public Map<String, Integer> readWeights(CrowdRule rule) {
        try {
            Map<String, Integer> m = new LinkedHashMap<>();
            Map<?, ?> raw = json.readValue(rule.getWeightsJson(), Map.class);
            for (String k : FACTORS.keySet()) {
                Object v = raw.get(k);
                m.put(k, v == null ? 0 : ((Number) v).intValue());
            }
            return m;
        } catch (Exception e) {
            Map<String, Integer> def = new LinkedHashMap<>();
            def.put("deptCapacity", 22);
            def.put("itemMix", 18);
            def.put("slotLoad", 20);
            def.put("duration", 14);
            def.put("arrivalRate", 16);
            def.put("deviceDown", 10);
            return def;
        }
    }

    public Map<String, Integer> readThresholds(CrowdRule rule) {
        try {
            Map<?, ?> raw = json.readValue(rule.getThresholdsJson(), Map.class);
            Map<String, Integer> m = new LinkedHashMap<>();
            m.put("recommend", raw.get("recommend") == null ? 40 : ((Number) raw.get("recommend")).intValue());
            m.put("normal", raw.get("normal") == null ? 70 : ((Number) raw.get("normal")).intValue());
            return m;
        } catch (Exception e) {
            return Map.of("recommend", 40, "normal", 70);
        }
    }

    private int nz(Integer v) {
        return v == null ? 0 : v;
    }

    private double clamp(double v) {
        return Math.max(0, Math.min(1, v));
    }

    private double round(double v, int scale) {
        return BigDecimal.valueOf(v).setScale(scale, RoundingMode.HALF_UP).doubleValue();
    }
}
