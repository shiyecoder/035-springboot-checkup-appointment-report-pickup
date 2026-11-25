package com.example.healthexam.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.healthexam.entity.PaymentRecord;
import org.apache.ibatis.annotations.Mapper;

/** 线下缴费登记 Mapper */
@Mapper
public interface PaymentRecordMapper extends BaseMapper<PaymentRecord> {
}
