package com.example.healthexam.controller;

import com.example.healthexam.common.PageResult;
import com.example.healthexam.common.R;
import com.example.healthexam.entity.CrowdRule;
import com.example.healthexam.entity.CrowdScore;
import com.example.healthexam.security.UserContext;
import com.example.healthexam.service.CrowdService;
import com.example.healthexam.common.BizException;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/** 创新模块：拥挤度规则配置 + 计算留痕 + 人工调整 */
@RestController
@RequestMapping("/api/crowd")
@RequiredArgsConstructor
public class CrowdController {

    private final CrowdService crowdService;

    @GetMapping("/factors")
    public R<Map<String, String>> factors() {
        return R.ok(CrowdService.FACTORS);
    }

    @GetMapping("/rule")
    public R<Map<String, Object>> currentRule() {
        CrowdRule r = crowdService.activeRule();
        return R.ok(Map.of("rule", r,
                "weights", crowdService.readWeights(r),
                "thresholds", crowdService.readThresholds(r)));
    }

    @GetMapping("/rule/history")
    public R<List<CrowdRule>> history() {
        return R.ok(crowdService.rules());
    }

    @PostMapping("/rule")
    public R<CrowdRule> saveRule(@RequestBody Map<String, Object> body) {
        if (!UserContext.isAdmin()) {
            throw new BizException(403, "仅体检管理员可修改拥挤度规则");
        }
        @SuppressWarnings("unchecked")
        Map<String, Object> raw = (Map<String, Object>) body.get("weights");
        java.util.LinkedHashMap<String, Integer> weights = new java.util.LinkedHashMap<>();
        raw.forEach((k, v) -> weights.put(k, Integer.parseInt(String.valueOf(v))));
        @SuppressWarnings("unchecked")
        Map<String, Object> rawTh = body.get("thresholds") == null ? null : (Map<String, Object>) body.get("thresholds");
        java.util.LinkedHashMap<String, Integer> th = null;
        if (rawTh != null) {
            final java.util.LinkedHashMap<String, Integer> target = new java.util.LinkedHashMap<>();
            rawTh.forEach((k, v) -> target.put(k, Integer.parseInt(String.valueOf(v))));
            th = target;
        }
        return R.ok(crowdService.saveRule(weights, th));
    }

    /** 计算：slotId + 可选 appId；preview=true 时不落库 */
    @PostMapping("/evaluate")
    public R<CrowdScore> evaluate(@RequestBody Map<String, Object> body) {
        Long slotId = Long.valueOf(String.valueOf(body.get("slotId")));
        Long appId = body.get("appId") == null ? null : Long.valueOf(String.valueOf(body.get("appId")));
        boolean preview = Boolean.parseBoolean(String.valueOf(body.getOrDefault("preview", "false")));
        return R.ok(crowdService.evaluate(slotId, appId, !preview));
    }

    @GetMapping("/score/page")
    public R<PageResult<CrowdScore>> page(@RequestParam(defaultValue = "1") long pageNum,
                                          @RequestParam(defaultValue = "10") long pageSize,
                                          @RequestParam(required = false) String level,
                                          @RequestParam(required = false) String scoreNo) {
        return R.ok(crowdService.page(pageNum, pageSize, level, scoreNo));
    }

    @PostMapping("/score/{id}/adjust")
    public R<Void> adjust(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        crowdService.adjust(id,
                body.get("adjustedScore") == null ? null : Double.valueOf(String.valueOf(body.get("adjustedScore"))),
                body.get("reason") == null ? null : String.valueOf(body.get("reason")));
        return R.ok();
    }

    @GetMapping("/effect")
    public R<Map<String, Object>> effect() {
        return R.ok(crowdService.effect());
    }
}
