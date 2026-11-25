package com.example.healthexam.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 数据字典
 * 表：sys_dict
 */
@Data
@TableName("sys_dict")
public class SysDict {
    /**  */
    @TableId(type = IdType.AUTO)
    private Long id;
    /** 字典类型 */
    private String dictType;
    /**  */
    private String dictLabel;
    /**  */
    private String dictValue;
    /**  */
    private Integer sort;
    /**  */
    private Integer status;
}
