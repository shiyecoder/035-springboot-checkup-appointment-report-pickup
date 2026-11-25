package com.example.healthexam.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 体检套餐
 * 表：exam_package
 */
@Data
@TableName("exam_package")
public class ExamPackage {
    /**  */
    @TableId(type = IdType.AUTO)
    private Long id;
    /** 套餐编号 */
    private String packageNo;
    /**  */
    private String packageName;
    /** 适用人群 */
    private String crowd;
    /** 执行科室汇总 */
    private String deptSummary;
    /**  */
    private BigDecimal originalPrice;
    /**  */
    private BigDecimal discountPrice;
    /** 预计时长 */
    private Integer durationMin;
    /** 项目数（后端汇总计算） */
    private Integer itemCount;
    /** 封面相对路径 */
    private String coverImage;
    /** 预约说明 */
    private String bookingNotice;
    /**  */
    private LocalDate shelfStart;
    /**  */
    private LocalDate shelfEnd;
    /** ON_SHELF/DRAFT/OFF_SHELF */
    private String status;
    /** 乐观锁版本 */
    private Integer version;
    /**  */
    private Long createBy;
    /**  */
    private LocalDateTime createTime;
    /**  */
    private LocalDateTime updateTime;
    /**  */
    @TableLogic
    private Integer deleted;
}
