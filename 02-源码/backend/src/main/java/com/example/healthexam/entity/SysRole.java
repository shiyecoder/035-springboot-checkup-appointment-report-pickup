package com.example.healthexam.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 角色表
 * 表：sys_role
 */
@Data
@TableName("sys_role")
public class SysRole {
    /**  */
    @TableId(type = IdType.AUTO)
    private Long id;
    /** 角色编码 */
    private String roleCode;
    /** 角色名称 */
    private String roleName;
    /** 数据范围 SELF/DEPT/ORG/ALL */
    private String dataScope;
    /**  */
    private String remark;
    /**  */
    private LocalDateTime createTime;
}
