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
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * 团检：批次、名单 Excel 导入（逐行校验并返回错误行）、集合统计。
 * 隐私：联络员只能查看到检与完成统计，不返回个人报告。
 */
@RestController
@RequestMapping("/api/group")
@RequiredArgsConstructor
public class GroupController {

    private final GroupBatchMapper batchMapper;
    private final GroupMemberMapper memberMapper;
    private final ExamPackageMapper packageMapper;
    private final ExamAppointmentMapper appMapper;

    @GetMapping("/page")
    public R<PageResult<GroupBatch>> page(@RequestParam(defaultValue = "1") long pageNum,
                                          @RequestParam(defaultValue = "10") long pageSize,
                                          @RequestParam(required = false) String keyword,
                                          @RequestParam(required = false) String status) {
        LambdaQueryWrapper<GroupBatch> w = new LambdaQueryWrapper<>();
        if ("ENTERPRISE".equals(UserContext.role())) {
            w.eq(GroupBatch::getOrgId, UserContext.get() == null ? -1 : UserContext.get().getOrgId());
        }
        if (keyword != null && !keyword.isBlank()) {
            w.and(x -> x.like(GroupBatch::getBatchNo, keyword).or().like(GroupBatch::getOrgName, keyword));
        }
        if (status != null && !status.isBlank() && !"ALL".equals(status)) {
            w.eq(GroupBatch::getStatus, status);
        }
        w.orderByDesc(GroupBatch::getCreateTime);
        return R.ok(PageResult.of(batchMapper.selectPage(new Page<>(pageNum, pageSize), w)));
    }

    @PostMapping("/batch")
    public R<Map<String, Object>> createBatch(@RequestBody Map<String, Object> body) {
        Long orgId = Long.valueOf(String.valueOf(body.get("orgId")));
        Long packageId = Long.valueOf(String.valueOf(body.get("packageId")));
        ExamPackage pkg = packageMapper.selectById(packageId);
        if (pkg == null) {
            throw new BizException("套餐不存在");
        }
        LocalDate start = LocalDate.parse(String.valueOf(body.get("startDate")));
        LocalDate end = LocalDate.parse(String.valueOf(body.get("endDate")));
        if (end.isBefore(start)) {
            throw new BizException("结束日期不能早于开始日期");
        }
        int quota = Integer.parseInt(String.valueOf(body.getOrDefault("quota", "0")));
        if (quota <= 0) {
            throw new BizException("名额必须大于 0");
        }
        GroupBatch b = new GroupBatch();
        b.setBatchNo("GB-" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy")) + "-"
                + String.format("%03d", (int) (System.currentTimeMillis() % 900) + 100));
        b.setOrgId(orgId);
        b.setOrgName(String.valueOf(body.getOrDefault("orgName", "企业")));
        b.setPackageId(packageId);
        b.setStartDate(start);
        b.setEndDate(end);
        b.setQuota(quota);
        b.setJoinedCount(0);
        b.setCheckedCount(0);
        b.setContactName(String.valueOf(body.getOrDefault("contactName", "")));
        b.setContactPhone(String.valueOf(body.getOrDefault("contactPhone", "")));
        b.setStatus("PENDING");
        b.setCreateBy(UserContext.userId());
        b.setCreateTime(LocalDateTime.now());
        b.setUpdateTime(LocalDateTime.now());
        batchMapper.insert(b);
        return R.ok(Map.of("id", b.getId(), "batchNo", b.getBatchNo()));
    }

    @GetMapping("/{id}/members")
    public R<PageResult<GroupMember>> members(@PathVariable Long id,
                                              @RequestParam(defaultValue = "1") long pageNum,
                                              @RequestParam(defaultValue = "10") long pageSize,
                                              @RequestParam(required = false) String importStatus) {
        LambdaQueryWrapper<GroupMember> w = new LambdaQueryWrapper<GroupMember>()
                .eq(GroupMember::getBatchId, id);
        if (importStatus != null && !importStatus.isBlank() && !"ALL".equals(importStatus)) {
            w.eq(GroupMember::getImportStatus, importStatus);
        }
        w.orderByDesc(GroupMember::getId);
        return R.ok(PageResult.of(memberMapper.selectPage(new Page<>(pageNum, pageSize), w)));
    }

    /**
     * 名单导入：演示环境接收已解析的行数据（前端上传 Excel 后按模板解析成 rows）。
     * 逐行校验姓名/手机号/身份证长度/套餐编号，返回失败行与原因，成功行入库。
     */
    @PostMapping("/{id}/import")
    public R<Map<String, Object>> importMembers(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        GroupBatch batch = batchMapper.selectById(id);
        if (batch == null) {
            throw new BizException(404, "批次不存在");
        }
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> rows = (List<Map<String, Object>>) body.get("rows");
        if (rows == null || rows.isEmpty()) {
            throw new BizException("导入内容为空");
        }
        if (rows.size() > 500) {
            throw new BizException("单次导入不超过 500 行");
        }
        List<Map<String, Object>> errors = new ArrayList<>();
        int ok = 0;
        int rowNo = 0;
        for (Map<String, Object> row : rows) {
            rowNo++;
            String name = s(row.get("name"));
            String phone = s(row.get("phone"));
            String idCard = s(row.get("idCard"));
            String pkgNo = s(row.get("packageNo"));
            String err = null;
            if (name == null || name.isBlank()) {
                err = "姓名不能为空";
            } else if (phone != null && phone.length() != 11) {
                err = "手机号格式不合法（" + phone + "）";
            } else if (idCard != null && idCard.length() != 18) {
                err = "身份证号校验位错误（" + idCard + "）";
            } else if (pkgNo != null && !pkgNo.isBlank()) {
                Long c = packageMapper.selectCount(new LambdaQueryWrapper<ExamPackage>()
                        .eq(ExamPackage::getPackageNo, pkgNo));
                if (c == null || c == 0) {
                    err = "套餐编号 " + pkgNo + " 不存在";
                }
            }
            GroupMember m = new GroupMember();
            m.setBatchId(id);
            m.setMemberName(name == null ? "" : name.trim());
            m.setGender(s(row.get("gender")));
            m.setAge(row.get("age") == null ? null : Integer.parseInt(String.valueOf(row.get("age"))));
            m.setPhone(phone);
            m.setIdCard(idCard);
            m.setPackageId(batch.getPackageId());
            m.setImportStatus(err == null ? "SUCCESS" : "FAILED");
            m.setErrorMsg(err);
            m.setCreateTime(LocalDateTime.now());
            memberMapper.insert(m);
            if (err == null) {
                ok++;
            } else {
                errors.add(Map.of("row", rowNo, "name", name == null ? "" : name, "error", err));
            }
        }
        Long joined = memberMapper.selectCount(new LambdaQueryWrapper<GroupMember>()
                .eq(GroupMember::getBatchId, id).eq(GroupMember::getImportStatus, "SUCCESS"));
        if (joined != null && joined > batch.getQuota()) {
            throw new BizException("导入后人数 " + joined + " 超过批次名额 " + batch.getQuota() + "，请调整名额后重试");
        }
        batch.setJoinedCount(joined == null ? 0 : joined.intValue());
        batch.setUpdateTime(LocalDateTime.now());
        batchMapper.updateById(batch);
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("success", ok);
        out.put("failed", errors.size());
        out.put("errors", errors);
        return R.ok(out);
    }

    /** 集合统计：到检 / 完成按批次口径统计，不含任何个人报告内容 */
    @GetMapping("/{id}/stats")
    public R<Map<String, Object>> stats(@PathVariable Long id) {
        GroupBatch batch = batchMapper.selectById(id);
        if (batch == null) {
            throw new BizException(404, "批次不存在");
        }
        Long joined = memberMapper.selectCount(new LambdaQueryWrapper<GroupMember>()
                .eq(GroupMember::getBatchId, id).eq(GroupMember::getImportStatus, "SUCCESS"));
        List<ExamAppointment> apps = appMapper.selectList(new LambdaQueryWrapper<ExamAppointment>()
                .eq(ExamAppointment::getBatchId, id));
        long checked = apps.stream().filter(a -> List.of("CHECKING", "WAIT_REPORT", "WAIT_PICKUP", "COMPLETED")
                .contains(a.getStatus())).count();
        long done = apps.stream().filter(a -> "COMPLETED".equals(a.getStatus())).count();
        long supplement = apps.stream().filter(a -> "WAIT_REPORT".equals(a.getStatus())).count();
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("batch", batch);
        m.put("joined", joined);
        m.put("checked", checked);
        m.put("completed", done);
        m.put("supplement", supplement);
        m.put("checkinRate", joined == null || joined == 0 ? 0 : Math.round(checked * 1000.0 / joined) / 10.0);
        return R.ok(m);
    }

    private String s(Object o) {
        return o == null ? null : String.valueOf(o);
    }
}
