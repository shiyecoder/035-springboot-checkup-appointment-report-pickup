package com.example.healthexam.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 拥挤度规则版本
 * 表：crowd_rule
 */
@Data
@TableName("crowd_rule")
public class CrowdRule {
    /**  */
    @TableId(type = IdType.AUTO)
    private Long id;
    /** 规则版本 */
    private Integer versionNo;
    /** 六项权重，合计 100 */
    private String weightsJson;
    /** 阈值配置 */
    private String thresholdsJson;
    /** ACTIVE/HISTORY/DRAFT */
    private String status;
    /**  */
    private LocalDateTime effectiveTime;
    /**  */
    private Long createBy;
    /**  */
    private String remark;
    /**  */
    private LocalDateTime createTime;
}
