package com.example.healthexam.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 套餐项目关联
 * 表：exam_package_item
 */
@Data
@TableName("exam_package_item")
public class ExamPackageItem {
    /**  */
    @TableId(type = IdType.AUTO)
    private Long id;
    /**  */
    private Long packageId;
    /**  */
    private Long itemId;
    /**  */
    private Integer sort;
}
