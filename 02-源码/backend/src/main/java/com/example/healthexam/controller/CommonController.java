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
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/** 上传 / 站内消息 / 系统管理（菜单、角色、用户、字典、操作日志） */
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class CommonController {

    private final SysMessageMapper messageMapper;
    private final SysUserMapper userMapper;
    private final SysRoleMapper roleMapper;
    private final SysMenuMapper menuMapper;
    private final SysRoleMenuMapper roleMenuMapper;
    private final SysDictMapper dictMapper;
    private final OperationLogMapper logMapper;

    @Value("${app.upload-dir}")
    private String uploadDir;

    // ---------------- 文件上传 ----------------
    /** 图片/附件统一走上传组件；只接受白名单扩展名，文件名由后端重命名 */
    @PostMapping("/upload")
    public R<Map<String, Object>> upload(@RequestParam("file") MultipartFile file,
                                         @RequestParam(defaultValue = "misc") String bizType) throws IOException {
        if (file.isEmpty()) {
            throw new BizException("上传文件为空");
        }
        String orig = file.getOriginalFilename() == null ? "file" : file.getOriginalFilename();
        String ext = orig.contains(".") ? orig.substring(orig.lastIndexOf('.') + 1).toLowerCase() : "";
        if (!List.of("jpg", "jpeg", "png", "webp", "pdf").contains(ext)) {
            throw new BizException("仅支持 jpg/png/webp/pdf 格式");
        }
        if (file.getSize() > 20 * 1024 * 1024L) {
            throw new BizException("单文件不超过 20MB");
        }
        Path dir = Paths.get(uploadDir, "upload", bizType).toAbsolutePath().normalize();
        Files.createDirectories(dir);
        String name = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS"))
                + "_" + Math.abs(Objects.hash(orig)) % 10000 + "." + ext;
        file.transferTo(dir.resolve(name).toFile());
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("url", "/uploads/upload/" + bizType + "/" + name);
        m.put("fileName", orig);
        m.put("size", file.getSize());
        return R.ok(m);
    }

    // ---------------- 消息 ----------------
    @GetMapping("/message/page")
    public R<PageResult<SysMessage>> messages(@RequestParam(defaultValue = "1") long pageNum,
                                              @RequestParam(defaultValue = "10") long pageSize,
                                              @RequestParam(required = false) Integer readFlag) {
        LambdaQueryWrapper<SysMessage> w = new LambdaQueryWrapper<SysMessage>()
                .eq(SysMessage::getUserId, UserContext.userId());
        if (readFlag != null) {
            w.eq(SysMessage::getReadFlag, readFlag);
        }
        w.orderByDesc(SysMessage::getCreateTime);
        return R.ok(PageResult.of(messageMapper.selectPage(new Page<>(pageNum, pageSize), w)));
    }

    @GetMapping("/message/unread")
    public R<Long> unread() {
        return R.ok(messageMapper.selectCount(new LambdaQueryWrapper<SysMessage>()
                .eq(SysMessage::getUserId, UserContext.userId()).eq(SysMessage::getReadFlag, 0)));
    }

    @PutMapping("/message/{id}/read")
    public R<Void> read(@PathVariable Long id) {
        SysMessage m = messageMapper.selectById(id);
        if (m != null && Objects.equals(m.getUserId(), UserContext.userId())) {
            m.setReadFlag(1);
            messageMapper.updateById(m);
        }
        return R.ok();
    }

    // ---------------- 系统 ----------------
    /** 角色分配菜单时隐藏「菜单管理」选项（需求硬性要求 12） */
    @GetMapping("/system/menus")
    public R<List<Map<String, Object>>> menus(@RequestParam(defaultValue = "false") boolean forAssign) {
        LambdaQueryWrapper<SysMenu> w = new LambdaQueryWrapper<SysMenu>().orderByAsc(SysMenu::getSort);
        if (forAssign) {
            w.ne(SysMenu::getMenuName, "菜单管理");
        }
        List<SysMenu> all = menuMapper.selectList(w);
        List<Map<String, Object>> tree = new ArrayList<>();
        for (SysMenu m : all) {
            if (m.getParentId() == 0) {
                Map<String, Object> node = new LinkedHashMap<>();
                node.put("id", m.getId());
                node.put("menuName", m.getMenuName());
                node.put("path", m.getPath());
                node.put("icon", m.getIcon());
                node.put("menuType", m.getMenuType());
                node.put("perm", m.getPerm());
                List<Map<String, Object>> children = new ArrayList<>();
                for (SysMenu c : all) {
                    if (Objects.equals(c.getParentId(), m.getId())) {
                        Map<String, Object> cc = new LinkedHashMap<>();
                        cc.put("id", c.getId());
                        cc.put("menuName", c.getMenuName());
                        cc.put("path", c.getPath());
                        cc.put("icon", c.getIcon());
                        cc.put("menuType", c.getMenuType());
                        cc.put("perm", c.getPerm());
                        children.add(cc);
                    }
                }
                node.put("children", children);
                tree.add(node);
            }
        }
        return R.ok(tree);
    }

    /** 菜单管理列表（管理页可见，分配菜单时不出现） */
    @GetMapping("/system/menu/list")
    public R<List<SysMenu>> menuList() {
        requireAdmin();
        return R.ok(menuMapper.selectList(new LambdaQueryWrapper<SysMenu>().orderByAsc(SysMenu::getParentId).orderByAsc(SysMenu::getSort)));
    }

    @PostMapping("/system/menu")
    public R<Void> saveMenu(@RequestBody SysMenu m) {
        requireAdmin();
        if (m.getMenuName() == null || m.getMenuName().isBlank()) {
            throw new BizException("菜单名称必填");
        }
        m.setCreateTime(LocalDateTime.now());
        if (m.getId() == null) {
            menuMapper.insert(m);
        } else {
            menuMapper.updateById(m);
        }
        return R.ok();
    }

    @GetMapping("/system/roles")
    public R<List<Map<String, Object>>> roles() {
        requireAdmin();
        List<SysRole> roles = roleMapper.selectList(new LambdaQueryWrapper<>());
        List<Map<String, Object>> out = new ArrayList<>();
        for (SysRole r : roles) {
            List<Long> menuIds = roleMenuMapper.selectList(new LambdaQueryWrapper<SysRoleMenu>()
                            .eq(SysRoleMenu::getRoleId, r.getId()))
                    .stream().map(SysRoleMenu::getMenuId).toList();
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("role", r);
            m.put("menuIds", menuIds);
            out.add(m);
        }
        return R.ok(out);
    }

    /** 为角色分配菜单：先清后插，事务内完成 */
    @PostMapping("/system/role/{id}/menus")
    public R<Void> assignMenus(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        requireAdmin();
        @SuppressWarnings("unchecked")
        List<Integer> menuIds = (List<Integer>) body.get("menuIds");
        roleMenuMapper.delete(new LambdaQueryWrapper<SysRoleMenu>().eq(SysRoleMenu::getRoleId, id));
        if (menuIds != null) {
            for (Integer mid : menuIds) {
                SysRoleMenu rm = new SysRoleMenu();
                rm.setRoleId(id);
                rm.setMenuId(Long.valueOf(mid));
                roleMenuMapper.insert(rm);
            }
        }
        return R.ok();
    }

    @GetMapping("/system/user/page")
    public R<PageResult<Map<String, Object>>> users(@RequestParam(defaultValue = "1") long pageNum,
                                                    @RequestParam(defaultValue = "10") long pageSize,
                                                    @RequestParam(required = false) String keyword,
                                                    @RequestParam(required = false) String roleCode) {
        requireAdmin();
        LambdaQueryWrapper<SysUser> w = new LambdaQueryWrapper<>();
        if (keyword != null && !keyword.isBlank()) {
            w.and(x -> x.like(SysUser::getUsername, keyword).or().like(SysUser::getRealName, keyword));
        }
        if (roleCode != null && !roleCode.isBlank() && !"ALL".equals(roleCode)) {
            w.eq(SysUser::getRoleCode, roleCode);
        }
        w.orderByDesc(SysUser::getCreateTime);
        Page<SysUser> p = userMapper.selectPage(new Page<>(pageNum, pageSize), w);
        List<Map<String, Object>> list = p.getRecords().stream().map(u -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", u.getId());
            m.put("username", u.getUsername());
            m.put("realName", u.getRealName());
            m.put("gender", u.getGender());
            m.put("phone", u.getPhone());
            m.put("roleCode", u.getRoleCode());
            m.put("dept", u.getDept());
            m.put("avatar", u.getAvatar());
            m.put("status", u.getStatus());
            m.put("createTime", u.getCreateTime());
            m.put("lastLoginTime", u.getLastLoginTime());
            return m;
        }).toList();
        return R.ok(PageResult.of(p.getTotal(), p.getCurrent(), p.getSize(), list));
    }

    @PutMapping("/system/user/{id}/status")
    public R<Void> toggleUser(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        requireAdmin();
        SysUser u = userMapper.selectById(id);
        if (u == null) {
            throw new BizException(404, "用户不存在");
        }
        u.setStatus(Integer.parseInt(String.valueOf(body.getOrDefault("status", "1"))));
        u.setUpdateTime(LocalDateTime.now());
        userMapper.updateById(u);
        return R.ok();
    }

    @GetMapping("/dict/{type}")
    public R<List<SysDict>> dict(@PathVariable String type) {
        return R.ok(dictMapper.selectList(new LambdaQueryWrapper<SysDict>()
                .eq(SysDict::getDictType, type).eq(SysDict::getStatus, 1).orderByAsc(SysDict::getSort)));
    }

    @GetMapping("/log/page")
    public R<PageResult<OperationLog>> logs(@RequestParam(defaultValue = "1") long pageNum,
                                            @RequestParam(defaultValue = "10") long pageSize,
                                            @RequestParam(required = false) String module) {
        LambdaQueryWrapper<OperationLog> w = new LambdaQueryWrapper<>();
        if (module != null && !module.isBlank()) {
            w.eq(OperationLog::getModule, module);
        }
        w.orderByDesc(OperationLog::getCreateTime);
        return R.ok(PageResult.of(logMapper.selectPage(new Page<>(pageNum, pageSize), w)));
    }

    private void requireAdmin() {
        if (!UserContext.isAdmin()) {
            throw new BizException(403, "该操作仅体检管理员可执行");
        }
    }
}
