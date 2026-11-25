package com.example.healthexam.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 体检报告归档（多版本）
 * 表：exam_report
 */
@Data
@TableName("exam_report")
public class ExamReport {
    /**  */
    @TableId(type = IdType.AUTO)
    private Long id;
    /**  */
    private Long appId;
    /** 版本号 */
    private Integer versionNo;
    /** 存储相对路径（不对外暴露） */
    private String filePath;
    /** 原始文件名 */
    private String fileName;
    /** 签发日期 */
    private LocalDate issueDate;
    /** 总检医师 */
    private String doctorName;
    /** 1 最新有效版 */
    private Integer isLatest;
    /** ARCHIVED/VOID */
    private String status;
    /**  */
    private Long uploaderId;
    /**  */
    private LocalDateTime createTime;
}
