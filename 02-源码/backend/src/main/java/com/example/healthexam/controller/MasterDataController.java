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
import java.util.*;

/** 主数据：体检套餐 / 检查项目 / 时段容量 */
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class MasterDataController {

    private final ExamPackageMapper packageMapper;
    private final ExamPackageItemMapper packageItemMapper;
    private final ExamItemMapper itemMapper;
    private final AppointmentSlotMapper slotMapper;
    private final SysOrgMapper orgMapper;

    // ---------------- 套餐 ----------------
    @GetMapping("/package/page")
    public R<PageResult<ExamPackage>> packagePage(@RequestParam(defaultValue = "1") long pageNum,
                                                  @RequestParam(defaultValue = "10") long pageSize,
                                                  @RequestParam(required = false) String keyword,
                                                  @RequestParam(required = false) String status,
                                                  @RequestParam(required = false) String crowd) {
        LambdaQueryWrapper<ExamPackage> w = new LambdaQueryWrapper<>();
        if (keyword != null && !keyword.isBlank()) {
            w.and(x -> x.like(ExamPackage::getPackageName, keyword).or().like(ExamPackage::getPackageNo, keyword));
        }
        if (status != null && !status.isBlank() && !"ALL".equals(status)) {
            w.eq(ExamPackage::getStatus, status);
        } else if (status == null) {
            // 门户/普通用户只看已上架
            if (!UserContext.isAdmin()) {
                w.eq(ExamPackage::getStatus, "ON_SHELF");
            }
        }
        if (crowd != null && !crowd.isBlank()) {
            w.eq(ExamPackage::getCrowd, crowd);
        }
        w.orderByDesc(ExamPackage::getCreateTime);
        return R.ok(PageResult.of(packageMapper.selectPage(new Page<>(pageNum, pageSize), w)));
    }

    @GetMapping("/package/{id}")
    public R<Map<String, Object>> packageDetail(@PathVariable Long id) {
        ExamPackage p = packageMapper.selectById(id);
        if (p == null) {
            throw new BizException(404, "套餐不存在");
        }
        List<ExamPackageItem> links = packageItemMapper.selectList(new LambdaQueryWrapper<ExamPackageItem>()
                .eq(ExamPackageItem::getPackageId, id).orderByAsc(ExamPackageItem::getSort));
        List<ExamItem> items = new ArrayList<>();
        for (ExamPackageItem l : links) {
            ExamItem it = itemMapper.selectById(l.getItemId());
            if (it != null) {
                items.add(it);
            }
        }
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("package", p);
        m.put("items", items);
        return R.ok(m);
    }

    @PostMapping("/package")
    public R<Map<String, Object>> savePackage(@RequestBody Map<String, Object> body) {
        requireAdmin();
        ExamPackage p = new ExamPackage();
        p.setPackageNo(str(body.get("packageNo")));
        p.setPackageName(str(body.get("packageName")));
        p.setCrowd(str(body.get("crowd")));
        p.setDeptSummary(str(body.get("deptSummary")));
        p.setOriginalPrice(new java.math.BigDecimal(str(body.getOrDefault("originalPrice", "0"))));
        p.setDiscountPrice(new java.math.BigDecimal(str(body.getOrDefault("discountPrice", "0"))));
        p.setDurationMin(Integer.parseInt(str(body.getOrDefault("durationMin", "60"))));
        p.setCoverImage(str(body.get("coverImage")));
        p.setBookingNotice(str(body.get("bookingNotice")));
        p.setStatus(str(body.getOrDefault("status", "DRAFT")));
        p.setVersion(1);
        p.setCreateBy(UserContext.userId());
        p.setCreateTime(LocalDateTime.now());
        p.setUpdateTime(LocalDateTime.now());
        if (p.getPackageNo() == null || p.getPackageNo().isBlank()) {
            p.setPackageNo("PKG-" + System.currentTimeMillis() % 100000);
        }
        if (p.getPackageName() == null || p.getPackageName().isBlank()) {
            throw new BizException("套餐名称必填");
        }
        // 套餐编号唯一
        Long dup = packageMapper.selectCount(new LambdaQueryWrapper<ExamPackage>()
                .eq(ExamPackage::getPackageNo, p.getPackageNo()));
        if (dup != null && dup > 0) {
            throw new BizException("套餐编号已存在");
        }
        @SuppressWarnings("unchecked")
        List<Integer> itemIds = (List<Integer>) body.get("itemIds");
        if (itemIds == null || itemIds.isEmpty()) {
            throw new BizException("请至少选择一个检查项目");
        }
        p.setItemCount(itemIds.size());
        packageMapper.insert(p);
        int sort = 1;
        for (Integer iid : itemIds) {
            ExamPackageItem link = new ExamPackageItem();
            link.setPackageId(p.getId());
            link.setItemId(Long.valueOf(iid));
            link.setSort(sort++);
            packageItemMapper.insert(link);
        }
        return R.ok(Map.of("id", p.getId()));
    }

    @PutMapping("/package/{id}")
    public R<Void> updatePackage(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        requireAdmin();
        ExamPackage p = packageMapper.selectById(id);
        if (p == null) {
            throw new BizException(404, "套餐不存在");
        }
        if (body.get("packageName") != null) p.setPackageName(str(body.get("packageName")));
        if (body.get("crowd") != null) p.setCrowd(str(body.get("crowd")));
        if (body.get("originalPrice") != null) p.setOriginalPrice(new java.math.BigDecimal(str(body.get("originalPrice"))));
        if (body.get("discountPrice") != null) p.setDiscountPrice(new java.math.BigDecimal(str(body.get("discountPrice"))));
        if (body.get("durationMin") != null) p.setDurationMin(Integer.parseInt(str(body.get("durationMin"))));
        if (body.get("coverImage") != null) p.setCoverImage(str(body.get("coverImage")));
        if (body.get("bookingNotice") != null) p.setBookingNotice(str(body.get("bookingNotice")));
        if (body.get("status") != null) p.setStatus(str(body.get("status")));
        p.setUpdateTime(LocalDateTime.now());
        packageMapper.updateById(p);
        return R.ok();
    }

    @DeleteMapping("/package/{id}")
    public R<Void> deletePackage(@PathVariable Long id) {
        requireAdmin();
        // 已被业务单据引用则不允许删除
        Long used = packageMapper.selectById(id) == null ? 0L : 1L;
        if (used == 0) {
            throw new BizException(404, "套餐不存在");
        }
        packageMapper.deleteById(id);
        packageItemMapper.delete(new LambdaQueryWrapper<ExamPackageItem>().eq(ExamPackageItem::getPackageId, id));
        return R.ok();
    }

    // ---------------- 检查项目 ----------------
    @GetMapping("/item/page")
    public R<PageResult<ExamItem>> itemPage(@RequestParam(defaultValue = "1") long pageNum,
                                            @RequestParam(defaultValue = "10") long pageSize,
                                            @RequestParam(required = false) String keyword,
                                            @RequestParam(required = false) String dept) {
        LambdaQueryWrapper<ExamItem> w = new LambdaQueryWrapper<>();
        if (keyword != null && !keyword.isBlank()) {
            w.and(x -> x.like(ExamItem::getItemName, keyword).or().like(ExamItem::getItemNo, keyword));
        }
        if (dept != null && !dept.isBlank() && !"ALL".equals(dept)) {
            w.eq(ExamItem::getDept, dept);
        }
        w.orderByAsc(ExamItem::getItemNo);
        return R.ok(PageResult.of(itemMapper.selectPage(new Page<>(pageNum, pageSize), w)));
    }

    @GetMapping("/item/all")
    public R<List<ExamItem>> itemAll() {
        return R.ok(itemMapper.selectList(new LambdaQueryWrapper<ExamItem>().orderByAsc(ExamItem::getItemNo)));
    }

    @PostMapping("/item")
    public R<Void> saveItem(@RequestBody ExamItem item) {
        requireAdmin();
        if (item.getItemName() == null || item.getItemName().isBlank()) {
            throw new BizException("项目名称必填");
        }
        if (item.getDurationMin() != null && item.getDurationMin() < 0) {
            throw new BizException("执行时长不能为负数");
        }
        if (item.getStatus() == null) {
            item.setStatus(1);
        }
        item.setCreateTime(LocalDateTime.now());
        item.setUpdateTime(LocalDateTime.now());
        if (item.getId() == null) {
            if (item.getItemNo() == null) {
                item.setItemNo("IT-" + System.currentTimeMillis() % 100000);
            }
            itemMapper.insert(item);
        } else {
            itemMapper.updateById(item);
        }
        return R.ok();
    }

    @DeleteMapping("/item/{id}")
    public R<Void> deleteItem(@PathVariable Long id) {
        requireAdmin();
        itemMapper.deleteById(id);
        return R.ok();
    }

    // ---------------- 时段容量 ----------------
    @GetMapping("/slot/page")
    public R<PageResult<AppointmentSlot>> slotPage(@RequestParam(defaultValue = "1") long pageNum,
                                                   @RequestParam(defaultValue = "10") long pageSize,
                                                   @RequestParam(required = false) String dateFrom,
                                                   @RequestParam(required = false) String dateTo,
                                                   @RequestParam(required = false) String status) {
        LambdaQueryWrapper<AppointmentSlot> w = new LambdaQueryWrapper<>();
        if (dateFrom != null && !dateFrom.isBlank()) w.ge(AppointmentSlot::getSlotDate, LocalDate.parse(dateFrom));
        if (dateTo != null && !dateTo.isBlank()) w.le(AppointmentSlot::getSlotDate, LocalDate.parse(dateTo));
        if (status != null && !status.isBlank() && !"ALL".equals(status)) w.eq(AppointmentSlot::getStatus, status);
        if (dateFrom == null && dateTo == null) {
            w.ge(AppointmentSlot::getSlotDate, LocalDate.now().minusDays(1));
        }
        w.orderByAsc(AppointmentSlot::getSlotDate).orderByAsc(AppointmentSlot::getPeriod);
        return R.ok(PageResult.of(slotMapper.selectPage(new Page<>(pageNum, pageSize), w)));
    }

    /** 预约页取某日可约时段（含拥挤度提示，由前端调 evaluate 获取明细） */
    @GetMapping("/slot/available")
    public R<List<Map<String, Object>>> availableSlots(@RequestParam String date) {
        List<AppointmentSlot> list = slotMapper.selectList(new LambdaQueryWrapper<AppointmentSlot>()
                .eq(AppointmentSlot::getSlotDate, LocalDate.parse(date))
                .orderByAsc(AppointmentSlot::getPeriod));
        List<Map<String, Object>> out = new ArrayList<>();
        for (AppointmentSlot s : list) {
            int used = nz(s.getPersonalBooked()) + nz(s.getGroupBooked());
            int quota = nz(s.getPersonalQuota()) + nz(s.getGroupQuota());
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", s.getId());
            m.put("timeRange", s.getTimeRange());
            m.put("quota", quota);
            m.put("booked", used);
            m.put("remain", Math.max(0, quota - used));
            m.put("status", s.getStatus());
            m.put("loadRate", quota == 0 ? 0 : Math.round(used * 100.0 / quota));
            out.add(m);
        }
        return R.ok(out);
    }

    @PutMapping("/slot/{id}")
    public R<Void> updateSlot(@PathVariable Long id, @RequestBody AppointmentSlot body) {
        requireAdmin();
        AppointmentSlot s = slotMapper.selectById(id);
        if (s == null) {
            throw new BizException(404, "时段不存在");
        }
        if (body.getPersonalQuota() != null && body.getPersonalQuota() < s.getPersonalBooked()) {
            throw new BizException("个人名额不能小于已预约人数");
        }
        if (body.getGroupQuota() != null && body.getGroupQuota() < s.getGroupBooked()) {
            throw new BizException("团体名额不能小于已预约人数");
        }
        if (body.getPersonalQuota() != null) s.setPersonalQuota(body.getPersonalQuota());
        if (body.getGroupQuota() != null) s.setGroupQuota(body.getGroupQuota());
        if (body.getDeptCapacity() != null) s.setDeptCapacity(body.getDeptCapacity());
        if (body.getStatus() != null) s.setStatus(body.getStatus());
        s.setUpdateTime(LocalDateTime.now());
        slotMapper.updateById(s);
        return R.ok();
    }

    /** 批量生成未来 7 天时段（管理员一键排班） */
    @PostMapping("/slot/generate")
    public R<Map<String, Object>> generateSlots(@RequestBody Map<String, Object> body) {
        requireAdmin();
        int days = Integer.parseInt(str(body.getOrDefault("days", "7")));
        int created = 0;
        for (int d = 0; d < days; d++) {
            LocalDate day = LocalDate.now().plusDays(d + 1);
            for (String[] p : new String[][]{{"AM_0730", "07:30-09:00", "50", "20", "280"},
                    {"AM_0900", "09:00-10:30", "50", "20", "260"}, {"AM_1030", "10:30-11:30", "30", "10", "180"}}) {
                Long exists = slotMapper.selectCount(new LambdaQueryWrapper<AppointmentSlot>()
                        .eq(AppointmentSlot::getSlotDate, day).eq(AppointmentSlot::getPeriod, p[0]));
                if (exists != null && exists > 0) {
                    continue;
                }
                AppointmentSlot s = new AppointmentSlot();
                s.setSlotDate(day);
                s.setPeriod(p[0]);
                s.setTimeRange(p[1]);
                s.setPersonalQuota(Integer.parseInt(p[2]));
                s.setGroupQuota(Integer.parseInt(p[3]));
                s.setPersonalBooked(0);
                s.setGroupBooked(0);
                s.setDeptCapacity(Integer.parseInt(p[4]));
                s.setStatus("OPEN");
                s.setCreateTime(LocalDateTime.now());
                s.setUpdateTime(LocalDateTime.now());
                slotMapper.insert(s);
                created++;
            }
        }
        return R.ok(Map.of("created", created));
    }

    // ---------------- 组织 ----------------
    @GetMapping("/org/all")
    public R<List<SysOrg>> orgAll() {
        return R.ok(orgMapper.selectList(new LambdaQueryWrapper<SysOrg>().orderByAsc(SysOrg::getId)));
    }

    private void requireAdmin() {
        if (!UserContext.isAdmin()) {
            throw new BizException(403, "该操作仅体检管理员可执行");
        }
    }

    private String str(Object o) {
        return o == null ? null : String.valueOf(o);
    }

    private int nz(Integer v) {
        return v == null ? 0 : v;
    }
}
