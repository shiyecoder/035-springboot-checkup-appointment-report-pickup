package com.example.healthexam.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 团检批次
 * 表：group_batch
 */
@Data
@TableName("group_batch")
public class GroupBatch {
    /**  */
    @TableId(type = IdType.AUTO)
    private Long id;
    /**  */
    private String batchNo;
    /** 企业 */
    private Long orgId;
    /**  */
    private String orgName;
    /**  */
    private Long packageId;
    /**  */
    private LocalDate startDate;
    /**  */
    private LocalDate endDate;
    /**  */
    private Integer quota;
    /**  */
    private Integer joinedCount;
    /**  */
    private Integer checkedCount;
    /**  */
    private String contactName;
    /**  */
    private String contactPhone;
    /** PENDING/CHECKING/DONE */
    private String status;
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
