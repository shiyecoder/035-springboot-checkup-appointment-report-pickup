package com.example.healthexam.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 菜单表
 * 表：sys_menu
 */
@Data
@TableName("sys_menu")
public class SysMenu {
    /**  */
    @TableId(type = IdType.AUTO)
    private Long id;
    /** 父菜单 */
    private Long parentId;
    /**  */
    private String menuName;
    /** 前端路由 */
    private String path;
    /**  */
    private String icon;
    /** 权限标识 */
    private String perm;
    /** M 目录 C 菜单 B 按钮 */
    private String menuType;
    /**  */
    private Integer sort;
    /** 1 显示 0 隐藏 */
    private Integer visible;
    /**  */
    private LocalDateTime createTime;
}
