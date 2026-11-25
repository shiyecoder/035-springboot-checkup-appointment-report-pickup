package com.example.healthexam.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 角色菜单关联
 * 表：sys_role_menu
 */
@Data
@TableName("sys_role_menu")
public class SysRoleMenu {
    /**  */
    @TableId(type = IdType.AUTO)
    private Long id;
    /**  */
    private Long roleId;
    /**  */
    private Long menuId;
}
