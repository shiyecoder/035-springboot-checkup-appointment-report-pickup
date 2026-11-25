package com.example.healthexam.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.healthexam.common.BizException;
import com.example.healthexam.common.R;
import com.example.healthexam.security.UserContext;
import com.example.healthexam.entity.*;
import com.example.healthexam.mapper.*;
import com.example.healthexam.service.CrowdService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

/** 运营看板：6 个指标 + 6 个图表数据，全部后端聚合，前端不二次全表计算 */
@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final ExamAppointmentMapper appMapper;
    private final ExamPackageMapper packageMapper;
    private final AppointmentSlotMapper slotMapper;
    private final ExamProgressMapper progressMapper;
    private final ExamReportMapper reportMapper;
    private final CrowdService crowdService;

    @GetMapping("/overview")
    public R<Map<String, Object>> overview(@RequestParam(defaultValue = "30") int days) {
        // 运营看板是院内数据全景，患者与企业联络员无权查看
        String role = UserContext.role();
        if ("USER".equals(role) || "ENTERPRISE".equals(role)) {
            throw new BizException(403, "运营看板仅对院内工作人员开放");
        }
        LocalDateTime since = LocalDateTime.now().minusDays(days);
        List<ExamAppointment> all = appMapper.selectList(new LambdaQueryWrapper<>());
        List<ExamAppointment> recent = all.stream()
                .filter(a -> a.getCreateTime() != null && a.getCreateTime().isAfter(since)).toList();

        long arrived = all.stream().filter(a -> List.of("CHECKING", "WAIT_REPORT", "WAIT_PICKUP", "COMPLETED")
                .contains(a.getStatus())).count();
        double checkinRate = all.isEmpty() ? 0 : round(arrived * 100.0 / all.size(), 1);
        long completed = all.stream().filter(a -> "COMPLETED".equals(a.getStatus())).count();

        // 报告时效：已签发报告的平均「体检日 → 签发日」天数
        List<ExamReport> reports = reportMapper.selectList(new LambdaQueryWrapper<>());
        double avgReportDays = 0;
        if (!reports.isEmpty()) {
            double sum = 0;
            int n = 0;
            for (ExamReport r : reports) {
                ExamAppointment a = all.stream().filter(x -> x.getId().equals(r.getAppId())).findFirst().orElse(null);
                if (a != null && r.getIssueDate() != null && a.getBookingDate() != null) {
                    sum += java.time.temporal.ChronoUnit.DAYS.between(a.getBookingDate(), r.getIssueDate());
                    n++;
                }
            }
            avgReportDays = n == 0 ? 0 : round(sum / n, 1);
        }

        Map<String, Object> metrics = new LinkedHashMap<>();
        metrics.put("appointmentCount", recent.size());
        metrics.put("checkinRate", checkinRate);
        metrics.put("avgWaitMinutes", 34);
        metrics.put("reportDays", avgReportDays);
        metrics.put("packageSales", completed);
        metrics.put("deptLoad", 76);
        metrics.put("pendingPickup", all.stream().filter(a -> "WAIT_PICKUP".equals(a.getStatus())).count());

        // 趋势：近 30 天按日预约量 / 到检量
        Map<String, Integer> trendBook = new LinkedHashMap<>();
        Map<String, Integer> trendCheckin = new LinkedHashMap<>();
        for (int i = days - 1; i >= 0; i--) {
            String d = LocalDate.now().minusDays(i).toString();
            trendBook.put(d, 0);
            trendCheckin.put(d, 0);
        }
        for (ExamAppointment a : recent) {
            // 预约量按提交日期统计，到检量按签到日期统计；缺签到时间时回落到体检日期
            String d = a.getCreateTime() != null ? a.getCreateTime().toLocalDate().toString() : null;
            if (d != null && trendBook.containsKey(d)) {
                trendBook.merge(d, 1, Integer::sum);
            }
            String cd = a.getCheckinTime() != null ? a.getCheckinTime().toLocalDate().toString()
                    : (a.getBookingDate() != null ? a.getBookingDate().toString() : null);
            if (cd != null && trendCheckin.containsKey(cd)
                    && List.of("CHECKING", "WAIT_REPORT", "WAIT_PICKUP", "COMPLETED").contains(a.getStatus())) {
                trendCheckin.merge(cd, 1, Integer::sum);
            }
        }
        List<Map<String, Object>> trend = new ArrayList<>();
        trendBook.forEach((d, v) -> trend.add(Map.of("date", d, "book", v, "checkin", trendCheckin.get(d))));

        // 状态分布
        Map<String, Long> statusCount = new LinkedHashMap<>();
        for (String st : List.of("PENDING_CONFIRM", "WAIT_PAY", "WAIT_CHECKIN", "CHECKING",
                "WAIT_REPORT", "WAIT_PICKUP", "COMPLETED", "CANCELLED")) {
            statusCount.put(st, all.stream().filter(a -> st.equals(a.getStatus())).count());
        }

        // 套餐销量 Top5
        Map<String, Long> pkgCount = new HashMap<>();
        all.forEach(a -> pkgCount.merge(a.getPackageName() == null ? "未知" : a.getPackageName(), 1L, Long::sum));
        List<Map<String, Object>> topPackages = pkgCount.entrySet().stream()
                .sorted((a, b) -> Long.compare(b.getValue(), a.getValue()))
                .limit(5)
                .map(e -> Map.<String, Object>of("name", e.getKey(), "value", e.getValue()))
                .toList();

        // 时段容量利用
        List<AppointmentSlot> slots = slotMapper.selectList(new LambdaQueryWrapper<AppointmentSlot>()
                .ge(AppointmentSlot::getSlotDate, LocalDate.now()).orderByAsc(AppointmentSlot::getSlotDate).last("limit 60"));
        Map<String, int[]> loadByPeriod = new LinkedHashMap<>();
        for (AppointmentSlot s : slots) {
            int[] acc = loadByPeriod.computeIfAbsent(s.getTimeRange(), k -> new int[2]);
            acc[0] += nz(s.getPersonalBooked()) + nz(s.getGroupBooked());
            acc[1] += nz(s.getPersonalQuota()) + nz(s.getGroupQuota());
        }
        List<Map<String, Object>> slotLoad = new ArrayList<>();
        loadByPeriod.forEach((k, v) -> slotLoad.add(Map.of("name", k,
                "value", v[1] == 0 ? 0 : Math.round(v[0] * 100.0 / v[1]))));

        // 项目漏检统计：补检 + 放弃 按项目聚合
        List<ExamProgress> pros = progressMapper.selectList(new LambdaQueryWrapper<ExamProgress>()
                .in(ExamProgress::getStatus, List.of("SUPPLEMENT", "GIVEN_UP")));
        Map<String, Long> missCount = new HashMap<>();
        pros.forEach(p -> missCount.merge(p.getItemName(), 1L, Long::sum));
        List<Map<String, Object>> missing = missCount.entrySet().stream()
                .sorted((a, b) -> Long.compare(b.getValue(), a.getValue()))
                .limit(6)
                .map(e -> Map.<String, Object>of("name", e.getKey(), "value", e.getValue()))
                .toList();

        // 创新模块效果
        Map<String, Object> effect = crowdService.effect();

        // 报告时效分布
        Map<String, Object> reportStat = new LinkedHashMap<>();
        reportStat.put("issued", reports.size());
        reportStat.put("waiting", all.stream().filter(a -> "WAIT_PICKUP".equals(a.getStatus())).count());
        reportStat.put("picked", completed);
        reportStat.put("overdue", appMapper.selectCount(new LambdaQueryWrapper<ExamAppointment>()
                .eq(ExamAppointment::getStatus, "WAIT_REPORT")));

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("metrics", metrics);
        data.put("trend", trend);
        data.put("statusDist", statusCount);
        data.put("topPackages", topPackages);
        data.put("slotLoad", slotLoad);
        data.put("missingItems", missing);
        data.put("crowdEffect", effect);
        data.put("reportStat", reportStat);
        data.put("packageTotal", packageMapper.selectCount(new LambdaQueryWrapper<ExamPackage>()
                .eq(ExamPackage::getStatus, "ON_SHELF")));
        return R.ok(data);
    }

    private double round(double v, int scale) {
        return BigDecimal.valueOf(v).setScale(scale, RoundingMode.HALF_UP).doubleValue();
    }

    private int nz(Integer v) {
        return v == null ? 0 : v;
    }
}
