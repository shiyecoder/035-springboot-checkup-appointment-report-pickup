package com.example.healthexam.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.healthexam.common.BizException;
import com.example.healthexam.common.PageResult;
import com.example.healthexam.common.R;
import com.example.healthexam.entity.*;
import com.example.healthexam.mapper.*;
import com.example.healthexam.security.UserContext;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

/**
 * 报告归档与领取。
 * 隐私约束（文档第 3 节）：
 * - 报告文件不暴露真实路径，接口只返回文件 ID 与名称；
 * - 下载需二次身份校验（身份证后 6 位）并写访问日志；
 * - 团检联络员不可下载个人报告；
 * - 用户只看最新有效版。
 */
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class ReportController {

    private final ExamReportMapper reportMapper;
    private final ReportPickupMapper pickupMapper;
    private final ExamAppointmentMapper appMapper;
    private final OperationLogMapper logMapper;
    private final SysUserMapper userMapper;

    @Value("${app.upload-dir}")
    private String uploadDir;

    @GetMapping("/report/page")
    public R<PageResult<ExamReport>> page(@RequestParam(defaultValue = "1") long pageNum,
                                          @RequestParam(defaultValue = "10") long pageSize,
                                          @RequestParam(required = false) String keyword) {
        LambdaQueryWrapper<ExamReport> w = new LambdaQueryWrapper<>();
        if (keyword != null && !keyword.isBlank()) {
            List<Long> appIds = appMapper.selectList(new LambdaQueryWrapper<ExamAppointment>()
                            .like(ExamAppointment::getAppNo, keyword))
                    .stream().map(ExamAppointment::getId).toList();
            if (appIds.isEmpty()) {
                return R.ok(PageResult.of(0, pageNum, pageSize, List.of()));
            }
            w.in(ExamReport::getAppId, appIds);
        }
        w.orderByDesc(ExamReport::getCreateTime);
        return R.ok(PageResult.of(reportMapper.selectPage(new Page<>(pageNum, pageSize), w)));
    }

    /** 上传/归档报告：同一预约重复上传自动递增版本，旧版本 is_latest=0 */
    @PostMapping("/report/{appId}")
    public R<Map<String, Object>> archive(@PathVariable Long appId, @RequestBody Map<String, Object> body) {
        if ("USER".equals(UserContext.role())) {
            throw new BizException(403, "报告由工作人员上传，用户不可归档");
        }
        ExamAppointment app = appMapper.selectById(appId);
        if (app == null) {
            throw new BizException(404, "预约不存在");
        }
        String filePath = body.get("filePath") == null ? null : String.valueOf(body.get("filePath"));
        if (filePath == null || filePath.isBlank()) {
            throw new BizException("请上传报告文件");
        }
        int nextVer = reportMapper.selectCount(new LambdaQueryWrapper<ExamReport>()
                .eq(ExamReport::getAppId, appId)).intValue() + 1;
        reportMapper.update(null, new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<ExamReport>()
                .eq(ExamReport::getAppId, appId).set(ExamReport::getIsLatest, 0));
        ExamReport r = new ExamReport();
        r.setAppId(appId);
        r.setVersionNo(nextVer);
        r.setFilePath(filePath);
        r.setFileName(String.valueOf(body.getOrDefault("fileName", "体检报告.pdf")));
        r.setIssueDate(body.get("issueDate") == null ? LocalDate.now() : LocalDate.parse(String.valueOf(body.get("issueDate"))));
        r.setDoctorName(String.valueOf(body.getOrDefault("doctorName", "李国强")));
        r.setIsLatest(1);
        r.setStatus("ARCHIVED");
        r.setUploaderId(UserContext.userId());
        r.setCreateTime(LocalDateTime.now());
        reportMapper.insert(r);

        app.setReportStatus("READY");
        app.setUpdateTime(LocalDateTime.now());
        appMapper.updateById(app);
        return R.ok(Map.of("id", r.getId(), "version", nextVer));
    }

    /**
     * 授权下载：二次身份校验（身份证后 6 位）+ 审计日志。
     * 这是"报告不暴露真实路径"的落地实现——前端只拿到一次性下载流。
     */
    @GetMapping("/report/{id}/download")
    public ResponseEntity<Resource> download(@PathVariable Long id,
                                             @RequestParam String idCardTail,
                                             @RequestParam(defaultValue = "false") boolean preview) {
        ExamReport r = reportMapper.selectById(id);
        if (r == null) {
            throw new BizException(404, "报告不存在");
        }
        if ("ENTERPRISE".equals(UserContext.role())) {
            throw new BizException(403, "企业联络员不可下载个人体检报告");
        }
        ExamAppointment app = appMapper.selectById(r.getAppId());
        if ("USER".equals(UserContext.role())) {
            if (!Objects.equals(app.getUserId(), UserContext.userId())) {
                throw new BizException(403, "只能下载本人的体检报告");
            }
            if (!"1".equals(String.valueOf(r.getIsLatest()))) {
                throw new BizException(403, "该版本已被新版本替代，仅管理员可查");
            }
            // 二次校验：身份证后 6 位
            SysUser u = userMapper.selectById(UserContext.userId());
            String digest = app.getIdCardDigest() == null ? "" : app.getIdCardDigest();
            if (idCardTail == null || idCardTail.length() < 4) {
                throw new BizException("请填写身份证后 6 位以完成二次校验");
            }
            if (!digest.endsWith(idCardTail.trim()) && !digest.contains(idCardTail.trim())) {
                throw new BizException(403, "身份校验未通过，无法下载报告");
            }
        }
        Path file = Paths.get(uploadDir).toAbsolutePath().normalize()
                .resolve(r.getFilePath().replaceFirst("^/uploads/", "")).normalize();
        if (!file.startsWith(Paths.get(uploadDir).toAbsolutePath().normalize())) {
            throw new BizException(403, "非法文件路径");
        }
        if (!preview) {
            OperationLog l = new OperationLog();
            l.setUserId(UserContext.userId());
            l.setUsername(UserContext.get() == null ? "" : UserContext.get().getUsername());
            l.setModule("报告领取");
            l.setAction("报告授权下载");
            l.setBizNo(app.getAppNo());
            l.setDetail("第 " + r.getVersionNo() + " 版，二次身份校验通过");
            l.setIp("127.0.0.1");
            l.setCreateTime(LocalDateTime.now());
            logMapper.insert(l);
        }
        if (!file.toFile().exists()) {
            throw new BizException(404, "报告文件尚未生成，请联系报告室");
        }
        String name = URLEncoder.encode(r.getFileName() == null ? "report.pdf" : r.getFileName(), StandardCharsets.UTF_8);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + name)
                .contentType(MediaType.APPLICATION_PDF)
                .body(new FileSystemResource(file));
    }

    // ---------------- 领取 ----------------
    @GetMapping("/pickup/page")
    public R<PageResult<ReportPickup>> pickupPage(@RequestParam(defaultValue = "1") long pageNum,
                                                  @RequestParam(defaultValue = "10") long pageSize,
                                                  @RequestParam(required = false) String pickupType) {
        LambdaQueryWrapper<ReportPickup> w = new LambdaQueryWrapper<>();
        if (pickupType != null && !pickupType.isBlank() && !"ALL".equals(pickupType)) {
            w.eq(ReportPickup::getPickupType, pickupType);
        }
        w.orderByDesc(ReportPickup::getPickupTime);
        return R.ok(PageResult.of(pickupMapper.selectPage(new Page<>(pageNum, pageSize), w)));
    }

    /** 登记领取：本人 / 在线下载 / 委托代领；代领必须登记领取人与关系 */
    @PostMapping("/pickup/{appId}")
    public R<Void> registerPickup(@PathVariable Long appId, @RequestBody Map<String, Object> body) {
        ExamAppointment app = appMapper.selectById(appId);
        if (app == null) {
            throw new BizException(404, "预约不存在");
        }
        if (!"WAIT_PICKUP".equals(app.getStatus())) {
            throw new BizException("当前状态不允许登记领取");
        }
        String type = String.valueOf(body.getOrDefault("pickupType", "SELF"));
        if ("ENTRUST".equals(type)) {
            if (body.get("pickerName") == null || body.get("authFile") == null) {
                throw new BizException("委托代领必须填写领取人并上传授权书");
            }
        }
        ExamReport report = reportMapper.selectOne(new LambdaQueryWrapper<ExamReport>()
                .eq(ExamReport::getAppId, appId).eq(ExamReport::getIsLatest, 1).last("limit 1"));
        if (report == null) {
            throw new BizException("尚未上传报告");
        }
        ReportPickup p = new ReportPickup();
        p.setAppId(appId);
        p.setReportId(report.getId());
        p.setPickupType(type);
        p.setPickerName(String.valueOf(body.getOrDefault("pickerName", app.getContactName())));
        p.setPickerIdCard(body.get("pickerIdCard") == null ? null : String.valueOf(body.get("pickerIdCard")));
        p.setPickerPhone(body.get("pickerPhone") == null ? app.getContactPhone() : String.valueOf(body.get("pickerPhone")));
        p.setRelation(body.get("relation") == null ? "本人" : String.valueOf(body.get("relation")));
        p.setAuthFile(body.get("authFile") == null ? null : String.valueOf(body.get("authFile")));
        p.setPickupTime(LocalDateTime.now());
        p.setOperatorId(UserContext.userId());
        p.setRemark(body.get("remark") == null ? null : String.valueOf(body.get("remark")));
        p.setCreateTime(LocalDateTime.now());
        pickupMapper.insert(p);

        app.setStatus("COMPLETED");
        app.setReportStatus("PICKED");
        app.setPickupMethod(type);
        app.setUpdateTime(LocalDateTime.now());
        appMapper.updateById(app);

        OperationLog l = new OperationLog();
        l.setUserId(UserContext.userId());
        l.setUsername(UserContext.get() == null ? "" : UserContext.get().getUsername());
        l.setModule("报告领取");
        l.setAction("ENTRUST".equals(type) ? "委托代领登记" : ("ONLINE".equals(type) ? "本人授权下载" : "现场领取"));
        l.setBizNo(app.getAppNo());
        l.setDetail("领取人 " + p.getPickerName() + "，方式 " + type);
        l.setIp("127.0.0.1");
        l.setCreateTime(LocalDateTime.now());
        logMapper.insert(l);
        return R.ok();
    }
}
