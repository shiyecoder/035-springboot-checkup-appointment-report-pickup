package com.example.healthexam.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.healthexam.entity.AppointmentSlot;
import org.apache.ibatis.annotations.Mapper;

/** 时段容量 Mapper */
@Mapper
public interface AppointmentSlotMapper extends BaseMapper<AppointmentSlot> {
}
