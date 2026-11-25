package com.example.healthexam.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.healthexam.entity.ExamItem;
import org.apache.ibatis.annotations.Mapper;

/** 检查项目 Mapper */
@Mapper
public interface ExamItemMapper extends BaseMapper<ExamItem> {
}
