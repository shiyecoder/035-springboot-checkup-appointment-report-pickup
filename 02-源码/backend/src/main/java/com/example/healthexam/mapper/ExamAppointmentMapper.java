package com.example.healthexam.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.healthexam.entity.ExamAppointment;
import org.apache.ibatis.annotations.Mapper;

/** 体检预约与报告主单 Mapper */
@Mapper
public interface ExamAppointmentMapper extends BaseMapper<ExamAppointment> {
}
