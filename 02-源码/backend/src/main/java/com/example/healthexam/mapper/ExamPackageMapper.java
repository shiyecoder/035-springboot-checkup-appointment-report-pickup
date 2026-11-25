package com.example.healthexam.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.healthexam.entity.ExamPackage;
import org.apache.ibatis.annotations.Mapper;

/** 体检套餐 Mapper */
@Mapper
public interface ExamPackageMapper extends BaseMapper<ExamPackage> {
}
