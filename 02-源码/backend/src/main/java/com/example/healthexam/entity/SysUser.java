package com.example.healthexam.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 用户表
 * 表：sys_user
 */
@Data
@TableName("sys_user")
public class SysUser {
    /**  */
    @TableId(type = IdType.AUTO)
    private Long id;
    /** 登录账号 */
    private String username;
    /** BCrypt 密码 */
    private String password;
    /** 姓名 */
    private String realName;
    /** 性别 M/F */
    private String gender;
    /** 手机号 */
    private String phone;
    /** 身份证摘要（脱敏） */
    private String idCardMasked;
    /** 头像相对路径 */
    private String avatar;
    /** 角色编码 */
    private String roleCode;
    /** 所属组织 */
    private Long orgId;
    /** 所属科室 */
    private String dept;
    /** 1 正常 0 停用 */
    private Integer status;
    /**  */
    private LocalDateTime lastLoginTime;
    /**  */
    private LocalDateTime createTime;
    /**  */
    private LocalDateTime updateTime;
    /**  */
    @TableLogic
    private Integer deleted;
}
