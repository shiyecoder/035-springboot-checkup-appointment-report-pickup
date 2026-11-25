package com.example.healthexam.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 线下缴费登记
 * 表：payment_record
 */
@Data
@TableName("payment_record")
public class PaymentRecord {
    /**  */
    @TableId(type = IdType.AUTO)
    private Long id;
    /**  */
    private Long appId;
    /** 应收 */
    private BigDecimal receivable;
    /** 减免 */
    private BigDecimal discountAmount;
    /** 实收 */
    private BigDecimal received;
    /** CASH/WECHAT/ALIPAY/CARD/GROUP */
    private String payMethod;
    /** 凭证图片相对路径 */
    private String voucherImage;
    /**  */
    private BigDecimal refundAmount;
    /**  */
    private String refundReason;
    /**  */
    private Long operatorId;
    /**  */
    private LocalDateTime createTime;
}
