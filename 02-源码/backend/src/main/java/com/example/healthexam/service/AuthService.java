package com.example.healthexam.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.healthexam.common.BizException;
import com.example.healthexam.entity.SysMenu;
import com.example.healthexam.entity.SysRoleMenu;
import com.example.healthexam.entity.SysUser;
import com.example.healthexam.mapper.SysMenuMapper;
import com.example.healthexam.mapper.SysRoleMenuMapper;
import com.example.healthexam.mapper.SysUserMapper;
import com.example.healthexam.security.JwtUtil;
import com.example.healthexam.security.UserContext;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;

/** 登录 / 注册 / 个人信息 / 按角色动态菜单 */
@Service
@RequiredArgsConstructor
public class AuthService {

    private final SysUserMapper userMapper;
    private final SysMenuMapper menuMapper;
    private final SysRoleMenuMapper roleMenuMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    /** 登录：连续失败 5 次锁定 10 分钟（内存计数，单机足够） */
    private static final Map<String, int[]> FAIL = new HashMap<>();
    private static final Map<String, LocalDateTime> LOCK = new HashMap<>();

    public Map<String, Object> login(String username, String password) {
        SysUser user = userMapper.selectOne(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getUsername, username).last("limit 1"));
        if (user == null) {
            throw new BizException(401, "账号或密码错误");
        }
        LocalDateTime lockUntil = LOCK.get(username);
        if (lockUntil != null && lockUntil.isAfter(LocalDateTime.now())) {
            throw new BizException(401, "连续登录失败，账号已锁定，请稍后再试");
        }
        if (!passwordEncoder.matches(password, user.getPassword())) {
            int[] cnt = FAIL.computeIfAbsent(username, k -> new int[]{0});
            cnt[0]++;
            if (cnt[0] >= 5) {
                LOCK.put(username, LocalDateTime.now().plusMinutes(10));
                FAIL.remove(username);
                throw new BizException(401, "连续登录失败 5 次，账号锁定 10 分钟");
            }
            throw new BizException(401, "账号或密码错误");
        }
        FAIL.remove(username);
        LOCK.remove(username);
        if (user.getStatus() != null && user.getStatus() == 0) {
            throw new BizException(403, "账号已停用，请联系管理员");
        }
        user.setLastLoginTime(LocalDateTime.now());
        userMapper.updateById(user);
        Map<String, Object> data = new HashMap<>();
        data.put("token", jwtUtil.create(user.getId(), user.getUsername(), user.getRoleCode(), user.getOrgId()));
        data.put("user", safeUser(user));
        data.put("menus", menusOf(user.getRoleCode()));
        return data;
    }

    public Map<String, Object> register(Map<String, String> body) {
        String username = body.getOrDefault("username", "").trim();
        String password = body.getOrDefault("password", "");
        String realName = body.getOrDefault("realName", "").trim();
        if (username.length() < 3) {
            throw new BizException("账号至少 3 个字符");
        }
        if (password.length() < 6) {
            throw new BizException("密码至少 6 位");
        }
        if (realName.isEmpty()) {
            throw new BizException("请填写姓名");
        }
        Long exists = userMapper.selectCount(new LambdaQueryWrapper<SysUser>().eq(SysUser::getUsername, username));
        if (exists != null && exists > 0) {
            throw new BizException("该账号已被注册");
        }
        SysUser u = new SysUser();
        u.setUsername(username);
        u.setPassword(passwordEncoder.encode(password));
        u.setRealName(realName);
        u.setGender(body.getOrDefault("gender", "M"));
        u.setPhone(body.get("phone"));
        u.setRoleCode("USER");
        u.setStatus(1);
        u.setAvatar("/uploads/seed/avatar/01.jpg");
        u.setCreateTime(LocalDateTime.now());
        u.setUpdateTime(LocalDateTime.now());
        userMapper.insert(u);
        return Map.of("id", u.getId());
    }

    /** 当前登录人信息 + 菜单树 */
    public Map<String, Object> profile() {
        SysUser u = userMapper.selectById(UserContext.userId());
        if (u == null) {
            throw new BizException(401, "登录已失效");
        }
        return Map.of("user", safeUser(u), "menus", menusOf(u.getRoleCode()));
    }

    /** 按角色查菜单：菜单权限只决定可见性，写接口另在 service 校验 */
    public List<SysMenu> menusOf(String roleCode) {
        Long roleId = roleIdOf(roleCode);
        List<Long> menuIds = roleMenuMapper.selectList(new LambdaQueryWrapper<SysRoleMenu>()
                        .eq(SysRoleMenu::getRoleId, roleId))
                .stream().map(SysRoleMenu::getMenuId).toList();
        if (menuIds.isEmpty()) {
            return List.of();
        }
        return menuMapper.selectList(new LambdaQueryWrapper<SysMenu>()
                .in(SysMenu::getId, menuIds).eq(SysMenu::getVisible, 1)
                .orderByAsc(SysMenu::getSort));
    }

    /** 四种角色在库里的固定 id：ADMIN=1 FRONT=2 ENTERPRISE=3 USER=4 */
    public Long roleIdOf(String roleCode) {
        return switch (roleCode == null ? "USER" : roleCode) {
            case "ADMIN" -> 1L;
            case "FRONT" -> 2L;
            case "ENTERPRISE" -> 3L;
            default -> 4L;
        };
    }

    public Map<String, Object> safeUser(SysUser u) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", u.getId());
        m.put("username", u.getUsername());
        m.put("realName", u.getRealName());
        m.put("gender", u.getGender());
        m.put("phone", u.getPhone());
        m.put("avatar", u.getAvatar());
        m.put("roleCode", u.getRoleCode());
        m.put("orgId", u.getOrgId());
        m.put("dept", u.getDept());
        m.put("idCardMasked", u.getIdCardMasked());
        m.put("createTime", u.getCreateTime());
        return m;
    }

    /** 改资料（姓名/手机/头像） */
    public void updateProfile(Map<String, String> body) {
        SysUser u = userMapper.selectById(UserContext.userId());
        if (u == null) {
            throw new BizException(401, "登录已失效");
        }
        if (body.get("realName") != null && !body.get("realName").isBlank()) {
            u.setRealName(body.get("realName").trim());
        }
        if (body.get("phone") != null) {
            u.setPhone(body.get("phone"));
        }
        if (body.get("avatar") != null) {
            u.setAvatar(body.get("avatar"));
        }
        u.setUpdateTime(LocalDateTime.now());
        userMapper.updateById(u);
    }

    /** 改密码：校验原密码 */
    public void changePassword(String oldPwd, String newPwd) {
        SysUser u = userMapper.selectById(UserContext.userId());
        if (u == null) {
            throw new BizException(401, "登录已失效");
        }
        if (!passwordEncoder.matches(oldPwd, u.getPassword())) {
            throw new BizException("原密码不正确");
        }
        if (newPwd == null || newPwd.length() < 6) {
            throw new BizException("新密码至少 6 位");
        }
        u.setPassword(passwordEncoder.encode(newPwd));
        u.setUpdateTime(LocalDateTime.now());
        userMapper.updateById(u);
    }
}
