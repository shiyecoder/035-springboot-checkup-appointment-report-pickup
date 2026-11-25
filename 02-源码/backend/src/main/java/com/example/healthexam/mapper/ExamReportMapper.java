package com.example.healthexam.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.healthexam.entity.ExamReport;
import org.apache.ibatis.annotations.Mapper;

/** 体检报告归档（多版本） Mapper */
@Mapper
public interface ExamReportMapper extends BaseMapper<ExamReport> {
}
