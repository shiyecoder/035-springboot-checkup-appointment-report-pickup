package com.example.healthexam.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 站内消息
 * 表：sys_message
 */
@Data
@TableName("sys_message")
public class SysMessage {
    /**  */
    @TableId(type = IdType.AUTO)
    private Long id;
    /** 接收人 */
    private Long userId;
    /**  */
    private String title;
    /**  */
    private String content;
    /** 业务编号 */
    private String bizNo;
    /** APPOINTMENT/REPORT/PAYMENT */
    private String bizType;
    /**  */
    private Integer readFlag;
    /**  */
    private LocalDateTime createTime;
}
