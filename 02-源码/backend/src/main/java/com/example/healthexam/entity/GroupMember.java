package com.example.healthexam.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 团检人员名单
 * 表：group_member
 */
@Data
@TableName("group_member")
public class GroupMember {
    /**  */
    @TableId(type = IdType.AUTO)
    private Long id;
    /**  */
    private Long batchId;
    /**  */
    private String memberName;
    /**  */
    private String gender;
    /**  */
    private Integer age;
    /**  */
    private String phone;
    /**  */
    private String idCard;
    /**  */
    private Long packageId;
    /** SUCCESS/FAILED */
    private String importStatus;
    /**  */
    private String errorMsg;
    /**  */
    private LocalDateTime createTime;
}
