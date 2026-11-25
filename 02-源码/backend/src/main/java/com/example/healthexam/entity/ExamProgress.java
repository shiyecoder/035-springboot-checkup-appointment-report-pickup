package com.example.healthexam.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 项目执行进度（漏检/补检）
 * 表：exam_progress
 */
@Data
@TableName("exam_progress")
public class ExamProgress {
    /**  */
    @TableId(type = IdType.AUTO)
    private Long id;
    /**  */
    private Long appId;
    /**  */
    private Long itemId;
    /**  */
    private String itemName;
    /**  */
    private String dept;
    /** WAITING/CHECKING/DONE/GIVEN_UP/SUPPLEMENT */
    private String status;
    /**  */
    private LocalDateTime checkTime;
    /**  */
    private Long operatorId;
    /**  */
    private String remark;
    /** 补检预约日期 */
    private LocalDate supplementDate;
    /**  */
    private LocalDateTime createTime;
    /**  */
    private LocalDateTime updateTime;
}
