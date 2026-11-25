package com.example.healthexam.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 拥挤度计算留痕
 * 表：crowd_score
 */
@Data
@TableName("crowd_score")
public class CrowdScore {
    /**  */
    @TableId(type = IdType.AUTO)
    private Long id;
    /** 计算编号 */
    private String scoreNo;
    /**  */
    private Long appId;
    /**  */
    private Long slotId;
    /**  */
    private Long ruleId;
    /**  */
    private Integer ruleVersion;
    /** 输入快照 JSON */
    private String inputSnapshot;
    /** 各项得分明细 JSON */
    private String detailJson;
    /** 综合拥挤度 */
    private BigDecimal totalScore;
    /** RECOMMEND/NORMAL/NOT_RECOMMEND */
    private String level;
    /**  */
    private String mainFactors;
    /**  */
    private String unmetConditions;
    /**  */
    private String improveTip;
    /** 人工调整后分数 */
    private BigDecimal adjustedScore;
    /**  */
    private String adjustReason;
    /**  */
    private Long adjustBy;
    /**  */
    private LocalDateTime adjustTime;
    /**  */
    private LocalDateTime createTime;
}
