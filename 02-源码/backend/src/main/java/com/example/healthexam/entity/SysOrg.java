package com.example.healthexam.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 组织表
 * 表：sys_org
 */
@Data
@TableName("sys_org")
public class SysOrg {
    /** 主键 */
    @TableId(type = IdType.AUTO)
    private Long id;
    /** 组织/企业名称 */
    private String orgName;
    /** 类型 ENTERPRISE/HOSPITAL */
    private String orgType;
    /** 联系人 */
    private String contact;
    /** 联系电话 */
    private String phone;
    /** 创建时间 */
    private LocalDateTime createTime;
}
