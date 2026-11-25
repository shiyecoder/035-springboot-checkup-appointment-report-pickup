package com.example.healthexam.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.healthexam.entity.ReportPickup;
import org.apache.ibatis.annotations.Mapper;

/** 报告领取/代领登记 Mapper */
@Mapper
public interface ReportPickupMapper extends BaseMapper<ReportPickup> {
}
