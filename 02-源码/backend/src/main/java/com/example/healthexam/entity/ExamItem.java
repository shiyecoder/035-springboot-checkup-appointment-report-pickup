package com.example.healthexam.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 检查项目
 * 表：exam_item
 */
@Data
@TableName("exam_item")
public class ExamItem {
    /**  */
    @TableId(type = IdType.AUTO)
    private Long id;
    /** 项目编号 */
    private String itemNo;
    /** 项目名称 */
    private String itemName;
    /** 执行科室 */
    private String dept;
    /** 前置要求（空腹等） */
    private String preRequirement;
    /** ALL/M/F */
    private String genderLimit;
    /**  */
    private Integer ageMin;
    /**  */
    private Integer ageMax;
    /** 参考价 */
    private BigDecimal refPrice;
    /** 执行时长(分钟) */
    private Integer durationMin;
    /** 注意事项 */
    private String notice;
    /** 设备编码（停用判断用） */
    private String deviceCode;
    /**  */
    private Integer status;
    /**  */
    private LocalDateTime createTime;
    /**  */
    private LocalDateTime updateTime;
    /**  */
    @TableLogic
    private Integer deleted;
}
