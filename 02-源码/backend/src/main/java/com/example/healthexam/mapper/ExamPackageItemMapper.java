package com.example.healthexam.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.healthexam.entity.ExamPackageItem;
import org.apache.ibatis.annotations.Mapper;

/** 套餐项目关联 Mapper */
@Mapper
public interface ExamPackageItemMapper extends BaseMapper<ExamPackageItem> {
}
