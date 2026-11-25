package com.example.healthexam.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 报告领取/代领登记
 * 表：report_pickup
 */
@Data
@TableName("report_pickup")
public class ReportPickup {
    /**  */
    @TableId(type = IdType.AUTO)
    private Long id;
    /**  */
    private Long appId;
    /**  */
    private Long reportId;
    /** SELF/ONLINE/ENTRUST */
    private String pickupType;
    /** 领取人 */
    private String pickerName;
    /** 领取人证件摘要 */
    private String pickerIdCard;
    /**  */
    private String pickerPhone;
    /** 与体检人关系 */
    private String relation;
    /** 授权书图片 */
    private String authFile;
    /**  */
    private LocalDateTime pickupTime;
    /**  */
    private Long operatorId;
    /**  */
    private String remark;
    /**  */
    private LocalDateTime createTime;
}
