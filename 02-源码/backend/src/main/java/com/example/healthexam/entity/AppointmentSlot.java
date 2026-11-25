package com.example.healthexam.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 时段容量
 * 表：appointment_slot
 */
@Data
@TableName("appointment_slot")
public class AppointmentSlot {
    /**  */
    @TableId(type = IdType.AUTO)
    private Long id;
    /** 日期 */
    private LocalDate slotDate;
    /** 时段 AM_0730/AM_0900/AM_1030 */
    private String period;
    /** 时间范围 */
    private String timeRange;
    /** 个人名额 */
    private Integer personalQuota;
    /** 团体名额 */
    private Integer groupQuota;
    /** 个人已约 */
    private Integer personalBooked;
    /** 团体已约 */
    private Integer groupBooked;
    /** 科室最大接待量 */
    private Integer deptCapacity;
    /** OPEN/CLOSED */
    private String status;
    /**  */
    private LocalDateTime createTime;
    /**  */
    private LocalDateTime updateTime;
}
