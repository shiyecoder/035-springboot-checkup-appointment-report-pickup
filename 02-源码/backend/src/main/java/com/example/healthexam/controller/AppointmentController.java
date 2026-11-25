package com.example.healthexam.controller;

import com.example.healthexam.common.PageResult;
import com.example.healthexam.common.R;
import com.example.healthexam.entity.ExamAppointment;
import com.example.healthexam.service.AppointmentService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/** 体检预约与报告主单 */
@RestController
@RequestMapping("/api/appointment")
@RequiredArgsConstructor
public class AppointmentController {

    private final AppointmentService appointmentService;

    @GetMapping("/page")
    public R<PageResult<ExamAppointment>> page(@RequestParam(defaultValue = "1") long pageNum,
                                               @RequestParam(defaultValue = "10") long pageSize,
                                               @RequestParam(required = false) String status,
                                               @RequestParam(required = false) String keyword,
                                               @RequestParam(required = false) String dateFrom,
                                               @RequestParam(required = false) String dateTo,
                                               @RequestParam(required = false) Long packageId) {
        return R.ok(appointmentService.page(pageNum, pageSize, status, keyword, dateFrom, dateTo, packageId));
    }

    @GetMapping("/{id}")
    public R<Map<String, Object>> detail(@PathVariable Long id) {
        return R.ok(appointmentService.detail(id));
    }

    @PostMapping
    public R<Map<String, Object>> create(@RequestBody Map<String, Object> body) {
        return R.ok(appointmentService.create(body));
    }

    /** 状态流转：body = {target, reason} */
    @PostMapping("/{id}/transition")
    public R<Map<String, Object>> transition(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        String target = String.valueOf(body.get("target"));
        String reason = body.get("reason") == null ? null : String.valueOf(body.get("reason"));
        return R.ok(appointmentService.transition(id, target, reason));
    }

    @PostMapping("/{id}/reschedule")
    public R<Void> reschedule(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        Long slotId = Long.valueOf(String.valueOf(body.get("slotId")));
        appointmentService.reschedule(id, slotId, String.valueOf(body.getOrDefault("reason", "")));
        return R.ok();
    }

    /** 线下缴费登记 */
    @PostMapping("/{id}/payment")
    public R<Void> payment(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        appointmentService.payment(id, body);
        return R.ok();
    }

    /** 单项进度标记：已检/检查中/放弃/需补检 */
    @PutMapping("/progress/{progressId}")
    public R<Void> markProgress(@PathVariable Long progressId, @RequestBody Map<String, Object> body) {
        appointmentService.markProgress(progressId,
                String.valueOf(body.get("status")),
                body.get("remark") == null ? null : String.valueOf(body.get("remark")),
                body.get("supplementDate") == null ? null : String.valueOf(body.get("supplementDate")));
        return R.ok();
    }
}
