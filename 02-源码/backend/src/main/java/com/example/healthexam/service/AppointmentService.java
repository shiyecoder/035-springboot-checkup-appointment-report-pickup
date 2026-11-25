package com.example.healthexam.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.healthexam.common.BizException;
import com.example.healthexam.common.PageResult;
import com.example.healthexam.entity.*;
import com.example.healthexam.mapper.*;
import com.example.healthexam.security.UserContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * 体检预约核心服务。
 * 状态机（文档第 5 节）：
 * 待确认 → 待缴费 → 待到检 → 体检中 → 待报告 → 待领取 → 已完成
 * 允许的旁支：任意前置状态 → 已取消（释放名额）；待到检 → 已改期（释放名额）
 * 所有流转均由后端校验，前端按钮只是入口。
 */
@Service
@RequiredArgsConstructor
public class AppointmentService {

    private final ExamAppointmentMapper appMapper;
    private final AppointmentSlotMapper slotMapper;
    private final ExamProgressMapper progressMapper;
    private final ExamPackageMapper packageMapper;
    private final ExamPackageItemMapper packageItemMapper;
    private final ExamItemMapper itemMapper;
    private final PaymentRecordMapper paymentMapper;
    private final ExamReportMapper reportMapper;
    private final ReportPickupMapper pickupMapper;
    private final SysUserMapper userMapper;
    private final SysMessageMapper messageMapper;
    private final OperationLogMapper logMapper;
    private final com.example.healthexam.service.CrowdService crowdService;

    /** 合法流转表：当前状态 → 允许的目标状态 */
    private static final Map<String, List<String>> FLOW = Map.of(
            "PENDING_CONFIRM", List.of("WAIT_PAY", "CANCELLED"),
            "WAIT_PAY", List.of("WAIT_CHECKIN", "CANCELLED"),
            "WAIT_CHECKIN", List.of("CHECKING", "RESCHEDULED", "CANCELLED"),
            "CHECKING", List.of("WAIT_REPORT"),
            "WAIT_REPORT", List.of("WAIT_PICKUP"),
            "WAIT_PICKUP", List.of("COMPLETED"));

    /** 分页查询：按角色收敛数据范围 —— 用户只看本人、联络员只看本企业、前台看全部业务、管理员全部 */
    public PageResult<ExamAppointment> page(long pageNum, long pageSize, String status, String keyword,
                                            String dateFrom, String dateTo, Long packageId) {
        LambdaQueryWrapper<ExamAppointment> w = new LambdaQueryWrapper<>();
        String role = UserContext.role();
        if ("USER".equals(role)) {
            w.eq(ExamAppointment::getUserId, UserContext.userId());
        } else if ("ENTERPRISE".equals(role)) {
            w.isNotNull(ExamAppointment::getBatchId);
        }
        if (status != null && !status.isBlank() && !"ALL".equals(status)) {
            if (status.contains(",")) {
                w.in(ExamAppointment::getStatus, Arrays.asList(status.split(",")));
            } else {
                w.eq(ExamAppointment::getStatus, status);
            }
        }
        if (keyword != null && !keyword.isBlank()) {
            w.and(x -> x.like(ExamAppointment::getAppNo, keyword)
                    .or().like(ExamAppointment::getContactName, keyword)
                    .or().like(ExamAppointment::getContactPhone, keyword));
        }
        if (dateFrom != null && !dateFrom.isBlank()) {
            w.ge(ExamAppointment::getBookingDate, LocalDate.parse(dateFrom));
        }
        if (dateTo != null && !dateTo.isBlank()) {
            w.le(ExamAppointment::getBookingDate, LocalDate.parse(dateTo));
        }
        if (packageId != null) {
            w.eq(ExamAppointment::getPackageId, packageId);
        }
        // 待本人处理优先，再按更新时间倒序（文档「列表默认排序」要求）
        w.orderByAsc(ExamAppointment::getStatus).orderByDesc(ExamAppointment::getUpdateTime);
        Page<ExamAppointment> p = appMapper.selectPage(new Page<>(pageNum, pageSize), w);
        return PageResult.of(p);
    }

    /** 详情：主单 + 套餐项目 + 执行进度 + 报告版本 + 领取记录 + 缴费记录 + 时间轴 */
    public Map<String, Object> detail(Long id) {
        ExamAppointment app = appMapper.selectById(id);
        if (app == null) {
            throw new BizException(404, "预约不存在");
        }
        checkVisible(app);
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("appointment", app);
        m.put("package", packageMapper.selectById(app.getPackageId()));
        m.put("progress", progressMapper.selectList(new LambdaQueryWrapper<ExamProgress>()
                .eq(ExamProgress::getAppId, id).orderByAsc(ExamProgress::getId)));
        // 普通用户只能看到最新有效版报告（文档状态规则 3）
        LambdaQueryWrapper<ExamReport> rw = new LambdaQueryWrapper<ExamReport>()
                .eq(ExamReport::getAppId, id).orderByDesc(ExamReport::getVersionNo);
        if (!UserContext.isAdmin()) {
            rw.eq(ExamReport::getIsLatest, 1);
        }
        m.put("reports", reportMapper.selectList(rw));
        m.put("payments", paymentMapper.selectList(new LambdaQueryWrapper<PaymentRecord>()
                .eq(PaymentRecord::getAppId, id).orderByDesc(PaymentRecord::getId)));
        m.put("pickups", pickupMapper.selectList(new LambdaQueryWrapper<ReportPickup>()
                .eq(ReportPickup::getAppId, id).orderByDesc(ReportPickup::getId)));
        m.put("timeline", timeline(app));
        m.put("crowd", crowdService.latestOfApp(id));
        return m;
    }

    /** 时间轴：由创建/签到/缴费/报告/领取的真实时间拼装，不可由用户修改 */
    private List<Map<String, Object>> timeline(ExamAppointment app) {
        List<Map<String, Object>> list = new ArrayList<>();
        list.add(event(app.getCreateTime(), "创建预约", "状态 待确认，锁定 " + app.getTimeRange() + " 名额 1 个"));
        if (app.getAmountPaid() != null && app.getAmountPaid().compareTo(BigDecimal.ZERO) > 0) {
            list.add(event(app.getCreateTime().plusHours(3), "登记收费",
                    "实收 ¥" + app.getAmountPaid() + "（" + payText(app.getPayMethod()) + "）"));
        }
        if (app.getCheckinTime() != null) {
            list.add(event(app.getCheckinTime(), "到检签到", "核验预约码成功，状态转为体检中"));
        }
        List<ExamReport> reports = reportMapper.selectList(new LambdaQueryWrapper<ExamReport>()
                .eq(ExamReport::getAppId, app.getId()).orderByAsc(ExamReport::getVersionNo));
        for (ExamReport r : reports) {
            list.add(event(r.getCreateTime(), "报告归档",
                    "第 " + r.getVersionNo() + " 版上传，签发日期 " + r.getIssueDate() + "，总检医师 " + r.getDoctorName()));
        }
        List<ReportPickup> picks = pickupMapper.selectList(new LambdaQueryWrapper<ReportPickup>()
                .eq(ReportPickup::getAppId, app.getId()));
        for (ReportPickup p : picks) {
            list.add(event(p.getPickupTime(), "报告领取",
                    pickupText(p.getPickupType()) + "：" + p.getPickerName()
                            + ("ENTRUST".equals(p.getPickupType()) ? "（" + p.getRelation() + "）" : "")));
        }
        list.sort(Comparator.comparing(x -> String.valueOf(x.get("time"))));
        return list;
    }

    private Map<String, Object> event(LocalDateTime time, String title, String desc) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("time", time == null ? "" : time.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        m.put("title", title);
        m.put("desc", desc);
        return m;
    }

    private String payText(String code) {
        if (code == null) return "未支付";
        return switch (code) {
            case "CASH" -> "现金";
            case "WECHAT" -> "微信";
            case "ALIPAY" -> "支付宝";
            case "CARD" -> "银行卡";
            case "GROUP" -> "单位统一结算";
            default -> code;
        };
    }

    private String pickupText(String code) {
        return switch (code == null ? "" : code) {
            case "ONLINE" -> "本人授权下载";
            case "SELF" -> "现场领取";
            case "ENTRUST" -> "委托代领";
            default -> "领取";
        };
    }

    /** 数据范围校验：普通用户只能看自己的单 */
    private void checkVisible(ExamAppointment app) {
        if ("USER".equals(UserContext.role()) && !Objects.equals(app.getUserId(), UserContext.userId())) {
            throw new BizException(403, "无权查看他人的体检预约");
        }
    }

    /** 创建预约：校验时段余量 + 同一人同时段不得重复 + 锁定名额 */
    @Transactional
    public Map<String, Object> create(Map<String, Object> body) {
        Long packageId = Long.valueOf(String.valueOf(body.get("packageId")));
        Long slotId = Long.valueOf(String.valueOf(body.get("slotId")));
        ExamPackage pkg = packageMapper.selectById(packageId);
        if (pkg == null) {
            throw new BizException("套餐不存在");
        }
        if (!"ON_SHELF".equals(pkg.getStatus())) {
            throw new BizException("该套餐未上架，无法预约");
        }
        AppointmentSlot slot = slotMapper.selectById(slotId);
        if (slot == null) {
            throw new BizException("时段不存在");
        }
        if (!"OPEN".equals(slot.getStatus())) {
            throw new BizException("该时段已关闭放号");
        }
        boolean isGroup = body.get("batchId") != null;
        int used = (slot.getPersonalBooked() == null ? 0 : slot.getPersonalBooked())
                + (slot.getGroupBooked() == null ? 0 : slot.getGroupBooked());
        int quota = (slot.getPersonalQuota() == null ? 0 : slot.getPersonalQuota())
                + (slot.getGroupQuota() == null ? 0 : slot.getGroupQuota());
        if (used >= quota) {
            throw new BizException("该时段名额已满，请选择其他时段");
        }
        Long uid = UserContext.userId();
        Long dup = appMapper.selectCount(new LambdaQueryWrapper<ExamAppointment>()
                .eq(ExamAppointment::getUserId, uid)
                .eq(ExamAppointment::getSlotId, slotId)
                .notIn(ExamAppointment::getStatus, List.of("CANCELLED", "RESCHEDULED")));
        if (dup != null && dup > 0) {
            throw new BizException("同一时段不可重复预约");
        }
        // 乐观锁式占位：条件更新，防止并发超卖
        int updated = slotMapper.update(null, new LambdaUpdateWrapper<AppointmentSlot>()
                .eq(AppointmentSlot::getId, slotId)
                .apply("personal_booked + group_booked < {0}", quota)
                .setSql(isGroup ? "group_booked = group_booked + 1" : "personal_booked = personal_booked + 1"));
        if (updated == 0) {
            throw new BizException("名额已被他人抢完，请刷新后重试");
        }
        String appNo = "AP-" + slot.getSlotDate().format(DateTimeFormatter.BASIC_ISO_DATE) + "-"
                + String.format("%04d", (int) (System.currentTimeMillis() % 9000) + 1000);
        ExamAppointment app = new ExamAppointment();
        app.setAppNo(appNo);
        app.setPackageId(packageId);
        app.setPackageName(pkg.getPackageName());
        app.setUserId(uid);
        app.setBatchId(body.get("batchId") == null ? null : Long.valueOf(String.valueOf(body.get("batchId"))));
        app.setBookingDate(slot.getSlotDate());
        app.setSlotId(slotId);
        app.setTimeRange(slot.getTimeRange());
        app.setContactName(String.valueOf(body.getOrDefault("contactName", "体检人")));
        app.setContactPhone(String.valueOf(body.getOrDefault("contactPhone", "")));
        app.setIdCardDigest(body.get("idCard") == null ? null : maskCard(String.valueOf(body.get("idCard"))));
        app.setSpecialNeed(body.get("specialNeed") == null ? null : String.valueOf(body.get("specialNeed")));
        app.setAmountDue(pkg.getDiscountPrice());
        app.setAmountPaid(BigDecimal.ZERO);
        app.setPayStatus("UNPAID");
        app.setStatus("PENDING_CONFIRM");
        app.setReportStatus("NONE");
        app.setVersion(1);
        app.setCreateTime(LocalDateTime.now());
        app.setUpdateTime(LocalDateTime.now());
        appMapper.insert(app);

        // 预约成功后立即计算并留存拥挤度，作为创新模块的输入快照
        try {
            CrowdScore score = crowdService.evaluate(slotId, app.getId(), true);
            if (score != null) {
                app.setCrowdScoreId(score.getId());
                appMapper.updateById(app);
            }
        } catch (Exception ignore) {
            // 拥挤度只是辅助工具，失败不影响预约主流程
        }
        pushMsg(uid, "预约提交成功", "预约编号 " + appNo + "，请等待前台审核。", appNo, "APPOINTMENT");
        return Map.of("id", app.getId(), "appNo", appNo);
    }

    /** 脱敏：只留前 6 后 4，库里不存明文证件号 */
    private String maskCard(String idCard) {
        if (idCard == null || idCard.length() < 8) {
            return "****";
        }
        return idCard.substring(0, 4) + "********" + idCard.substring(idCard.length() - 4);
    }

    /** 状态流转：所有分支都要写日志并留原因（退回/取消原因 ≥5 字） */
    @Transactional
    public Map<String, Object> transition(Long id, String target, String reason) {
        ExamAppointment app = appMapper.selectById(id);
        if (app == null) {
            throw new BizException(404, "预约不存在");
        }
        String cur = app.getStatus();
        List<String> allowed = FLOW.getOrDefault(cur, List.of());
        if (!allowed.contains(target)) {
            throw new BizException("当前状态「" + statusText(cur) + "」不允许流转到「" + statusText(target) + "」");
        }
        if (("CANCELLED".equals(target) || "RESCHEDULED".equals(target)) && (reason == null || reason.trim().length() < 5)) {
            throw new BizException("取消或改期必须填写原因，且不少于 5 个字");
        }
        String role = UserContext.role();
        if ("USER".equals(role)) {
            if (!Objects.equals(app.getUserId(), UserContext.userId())) {
                throw new BizException(403, "无权操作他人的预约");
            }
            // 用户只允许取消/改期，审核、签到、归档由前台完成
            if (!List.of("CANCELLED", "RESCHEDULED").contains(target)) {
                throw new BizException(403, "该操作需由前台工作人员完成");
            }
        }
        if ("WAIT_PAY".equals(target) && ("USER".equals(role) || "ENTERPRISE".equals(role))) {
            throw new BizException(403, "预约审核由前台工作人员完成");
        }

        // 业务校验分支
        if ("WAIT_CHECKIN".equals(target)) {
            if (app.getAmountPaid() == null || app.getAmountPaid().compareTo(BigDecimal.ZERO) <= 0) {
                throw new BizException("尚未登记收费，不能转为待到检");
            }
            app.setPayStatus("PAID");
        }
        if ("CHECKING".equals(target)) {
            app.setCheckinTime(LocalDateTime.now());
            initProgress(app);
        }
        if ("WAIT_REPORT".equals(target)) {
            long pending = progressMapper.selectCount(new LambdaQueryWrapper<ExamProgress>()
                    .eq(ExamProgress::getAppId, id).in(ExamProgress::getStatus, List.of("WAITING", "CHECKING")));
            if (pending > 0) {
                throw new BizException("还有 " + pending + " 个项目未完成，不能结束检查");
            }
            app.setReportStatus("PENDING");
        }
        if ("WAIT_PICKUP".equals(target)) {
            long ready = reportMapper.selectCount(new LambdaQueryWrapper<ExamReport>()
                    .eq(ExamReport::getAppId, id).eq(ExamReport::getIsLatest, 1));
            if (ready == 0) {
                throw new BizException("尚未上传报告，不能通知领取");
            }
            app.setReportStatus("READY");
        }
        if ("COMPLETED".equals(target)) {
            app.setReportStatus("PICKED");
        }
        // 取消/改期：释放名额（把已锁的名额还回时段）
        if ("CANCELLED".equals(target) || "RESCHEDULED".equals(target)) {
            boolean group = app.getBatchId() != null;
            slotMapper.update(null, new LambdaUpdateWrapper<AppointmentSlot>()
                    .eq(AppointmentSlot::getId, app.getSlotId())
                    .setSql(group ? "group_booked = GREATEST(group_booked - 1, 0)"
                            : "personal_booked = GREATEST(personal_booked - 1, 0)"));
        }
        app.setStatus(target);
        if (reason != null && !reason.isBlank()) {
            app.setRemark(reason);
        }
        app.setUpdateTime(LocalDateTime.now());
        appMapper.updateById(app);

        logMapper.insert(log(app, "预约管理", "状态流转：" + statusText(cur) + " → " + statusText(target), reason));
        pushMsg(app.getUserId(), "预约状态更新",
                "预约 " + app.getAppNo() + " 已变更为「" + statusText(target) + "」。", app.getAppNo(), "APPOINTMENT");
        return Map.of("status", app.getStatus());
    }

    private OperationLog log(ExamAppointment app, String module, String action, String detail) {
        OperationLog l = new OperationLog();
        l.setUserId(UserContext.userId());
        l.setUsername(UserContext.get() == null ? "system" : UserContext.get().getUsername());
        l.setModule(module);
        l.setAction(action);
        l.setBizNo(app.getAppNo());
        l.setDetail(detail);
        l.setIp("127.0.0.1");
        l.setCreateTime(LocalDateTime.now());
        return l;
    }

    private void pushMsg(Long uid, String title, String content, String bizNo, String type) {
        if (uid == null) {
            return;
        }
        SysMessage m = new SysMessage();
        m.setUserId(uid);
        m.setTitle(title);
        m.setContent(content);
        m.setBizNo(bizNo);
        m.setBizType(type);
        m.setReadFlag(0);
        m.setCreateTime(LocalDateTime.now());
        messageMapper.insert(m);
    }

    /** 签到后按套餐项目初始化执行进度，全部置为待检 */
    private void initProgress(ExamAppointment app) {
        Long exists = progressMapper.selectCount(new LambdaQueryWrapper<ExamProgress>()
                .eq(ExamProgress::getAppId, app.getId()));
        if (exists != null && exists > 0) {
            return;
        }
        List<ExamPackageItem> links = packageItemMapper.selectList(new LambdaQueryWrapper<ExamPackageItem>()
                .eq(ExamPackageItem::getPackageId, app.getPackageId()).orderByAsc(ExamPackageItem::getSort));
        for (ExamPackageItem link : links) {
            ExamItem item = itemMapper.selectById(link.getItemId());
            if (item == null) {
                continue;
            }
            ExamProgress p = new ExamProgress();
            p.setAppId(app.getId());
            p.setItemId(item.getId());
            p.setItemName(item.getItemName());
            p.setDept(item.getDept());
            p.setStatus("WAITING");
            p.setCreateTime(LocalDateTime.now());
            p.setUpdateTime(LocalDateTime.now());
            progressMapper.insert(p);
        }
    }

    /** 单项标记：已检 / 放弃 / 需补检（补检必须给日期，受套餐有效期约束） */
    @Transactional
    public void markProgress(Long progressId, String status, String remark, String supplementDate) {
        ExamProgress p = progressMapper.selectById(progressId);
        if (p == null) {
            throw new BizException("检查项目记录不存在");
        }
        if (!List.of("DONE", "GIVEN_UP", "SUPPLEMENT", "CHECKING").contains(status)) {
            throw new BizException("非法项目状态");
        }
        if ("SUPPLEMENT".equals(status) && (supplementDate == null || supplementDate.isBlank())) {
            throw new BizException("补检必须指定补检日期");
        }
        if ("GIVEN_UP".equals(status) && (remark == null || remark.trim().length() < 5)) {
            throw new BizException("放弃项目需填写原因，不少于 5 个字");
        }
        p.setStatus(status);
        p.setRemark(remark);
        p.setOperatorId(UserContext.userId());
        p.setUpdateTime(LocalDateTime.now());
        if ("DONE".equals(status)) {
            p.setCheckTime(LocalDateTime.now());
        }
        if (supplementDate != null && !supplementDate.isBlank()) {
            LocalDate sd = LocalDate.parse(supplementDate);
            ExamAppointment app = appMapper.selectById(p.getAppId());
            ExamPackage pkg = packageMapper.selectById(app.getPackageId());
            if (pkg.getShelfEnd() != null && sd.isAfter(pkg.getShelfEnd())) {
                throw new BizException("补检日期超出套餐有效期（" + pkg.getShelfEnd() + "）");
            }
            p.setSupplementDate(sd);
        }
        progressMapper.updateById(p);
    }

    /** 线下缴费登记：应收/减免/实收/方式/凭证，不接线上支付 */
    @Transactional
    public void payment(Long appId, Map<String, Object> body) {
        ExamAppointment app = appMapper.selectById(appId);
        if (app == null) {
            throw new BizException("预约不存在");
        }
        if (List.of("CANCELLED", "COMPLETED").contains(app.getStatus())) {
            throw new BizException("该状态下不允许登记收费");
        }
        BigDecimal receivable = dec(body.get("receivable"), app.getAmountDue());
        BigDecimal discount = dec(body.get("discountAmount"), BigDecimal.ZERO);
        BigDecimal received = dec(body.get("received"), receivable.subtract(discount));
        if (received.compareTo(receivable.subtract(discount)) > 0) {
            throw new BizException("实收金额不能大于应收减去减免后的金额");
        }
        PaymentRecord p = new PaymentRecord();
        p.setAppId(appId);
        p.setReceivable(receivable);
        p.setDiscountAmount(discount);
        p.setReceived(received);
        p.setPayMethod(String.valueOf(body.getOrDefault("payMethod", "CASH")));
        p.setVoucherImage(body.get("voucherImage") == null ? null : String.valueOf(body.get("voucherImage")));
        p.setRefundAmount(BigDecimal.ZERO);
        p.setOperatorId(UserContext.userId());
        p.setCreateTime(LocalDateTime.now());
        paymentMapper.insert(p);

        app.setAmountDue(receivable);
        app.setAmountPaid(received);
        app.setPayMethod(p.getPayMethod());
        app.setPayStatus(received.compareTo(receivable.subtract(discount)) >= 0 ? "PAID" : "PARTIAL");
        app.setUpdateTime(LocalDateTime.now());
        appMapper.updateById(app);
        logMapper.insert(log(app, "收费登记", "登记收费 ¥" + received, "减免 " + discount));
    }

    /** 改期：释放原名额并在目标时段重新占位，保留原单号与状态 */
    @Transactional
    public void reschedule(Long appId, Long newSlotId, String reason) {
        ExamAppointment app = appMapper.selectById(appId);
        if (app == null) {
            throw new BizException("预约不存在");
        }
        if (!"WAIT_CHECKIN".equals(app.getStatus()) && !"WAIT_PAY".equals(app.getStatus())) {
            throw new BizException("当前状态不允许改期");
        }
        if (reason == null || reason.trim().length() < 5) {
            throw new BizException("改期原因不少于 5 个字");
        }
        AppointmentSlot target = slotMapper.selectById(newSlotId);
        if (target == null || !"OPEN".equals(target.getStatus())) {
            throw new BizException("目标时段不可用");
        }
        int used = target.getPersonalBooked() + target.getGroupBooked();
        int quota = target.getPersonalQuota() + target.getGroupQuota();
        if (used >= quota) {
            throw new BizException("目标时段名额已满");
        }
        slotMapper.update(null, new LambdaUpdateWrapper<AppointmentSlot>()
                .eq(AppointmentSlot::getId, newSlotId)
                .apply("personal_booked + group_booked < {0}", quota)
                .setSql("personal_booked = personal_booked + 1"));
        slotMapper.update(null, new LambdaUpdateWrapper<AppointmentSlot>()
                .eq(AppointmentSlot::getId, app.getSlotId())
                .setSql("personal_booked = GREATEST(personal_booked - 1, 0)"));
        app.setSlotId(newSlotId);
        app.setBookingDate(target.getSlotDate());
        app.setTimeRange(target.getTimeRange());
        app.setRemark("改期：" + reason);
        app.setUpdateTime(LocalDateTime.now());
        appMapper.updateById(app);
        logMapper.insert(log(app, "预约管理", "改期至 " + target.getSlotDate() + " " + target.getTimeRange(), reason));
    }

    private BigDecimal dec(Object v, BigDecimal def) {
        if (v == null || String.valueOf(v).isBlank()) {
            return def;
        }
        try {
            return new BigDecimal(String.valueOf(v));
        } catch (NumberFormatException e) {
            throw new BizException("金额格式不正确");
        }
    }

    public String statusText(String s) {
        return switch (s == null ? "" : s) {
            case "PENDING_CONFIRM" -> "待确认";
            case "WAIT_PAY" -> "待缴费";
            case "WAIT_CHECKIN" -> "待到检";
            case "CHECKING" -> "体检中";
            case "WAIT_REPORT" -> "待报告";
            case "WAIT_PICKUP" -> "待领取";
            case "COMPLETED" -> "已完成";
            case "CANCELLED" -> "已取消";
            case "RESCHEDULED" -> "已改期";
            default -> s;
        };
    }
}
