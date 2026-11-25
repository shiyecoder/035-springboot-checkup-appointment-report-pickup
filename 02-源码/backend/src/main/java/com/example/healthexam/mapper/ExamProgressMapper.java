package com.example.healthexam.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.healthexam.entity.ExamProgress;
import org.apache.ibatis.annotations.Mapper;

/** 项目执行进度（漏检/补检） Mapper */
@Mapper
public interface ExamProgressMapper extends BaseMapper<ExamProgress> {
}
