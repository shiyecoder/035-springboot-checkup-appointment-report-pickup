package com.example.healthexam.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 体检预约与报告主单
 * 表：exam_appointment
 */
@Data
@TableName("exam_appointment")
public class ExamAppointment {
    /**  */
    @TableId(type = IdType.AUTO)
    private Long id;
    /** 预约编号 */
    private String appNo;
    /**  */
    private Long packageId;
    /**  */
    private String packageName;
    /** 预约用户 */
    private Long userId;
    /** 团检批次 */
    private Long batchId;
    /**  */
    private LocalDate bookingDate;
    /**  */
    private Long slotId;
    /**  */
    private String timeRange;
    /**  */
    private String contactName;
    /**  */
    private String contactPhone;
    /** 身份证摘要，不存明文 */
    private String idCardDigest;
    /**  */
    private String specialNeed;
    /** 应收 */
    private BigDecimal amountDue;
    /** 实收 */
    private BigDecimal amountPaid;
    /** UNPAID/PARTIAL/PAID/REFUNDED */
    private String payStatus;
    /** CASH/WECHAT/ALIPAY/CARD/GROUP */
    private String payMethod;
    /** 见状态机 */
    private String status;
    /**  */
    private LocalDateTime checkinTime;
    /** NONE/PENDING/READY/PICKED */
    private String reportStatus;
    /** SELF/ONLINE/ENTRUST */
    private String pickupMethod;
    /** 关联拥挤度计算 */
    private Long crowdScoreId;
    /** 当前责任人（前台） */
    private Long ownerId;
    /**  */
    private String remark;
    /**  */
    private Integer version;
    /**  */
    private LocalDateTime createTime;
    /**  */
    private LocalDateTime updateTime;
    /**  */
    @TableLogic
    private Integer deleted;
}
