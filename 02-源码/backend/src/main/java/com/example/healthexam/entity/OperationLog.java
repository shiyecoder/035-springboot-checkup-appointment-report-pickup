package com.example.healthexam.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 操作日志（含报告下载审计）
 * 表：operation_log
 */
@Data
@TableName("operation_log")
public class OperationLog {
    /**  */
    @TableId(type = IdType.AUTO)
    private Long id;
    /**  */
    private Long userId;
    /**  */
    private String username;
    /**  */
    private String module;
    /**  */
    private String action;
    /**  */
    private String bizNo;
    /**  */
    private String detail;
    /**  */
    private String ip;
    /**  */
    private LocalDateTime createTime;
}
