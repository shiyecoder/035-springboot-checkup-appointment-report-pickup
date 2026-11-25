package com.example.healthexam.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.healthexam.entity.OperationLog;
import org.apache.ibatis.annotations.Mapper;

/** 操作日志（含报告下载审计） Mapper */
@Mapper
public interface OperationLogMapper extends BaseMapper<OperationLog> {
}
