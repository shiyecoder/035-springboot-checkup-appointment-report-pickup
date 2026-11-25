package com.example.healthexam.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.healthexam.entity.SysMessage;
import org.apache.ibatis.annotations.Mapper;

/** 站内消息 Mapper */
@Mapper
public interface SysMessageMapper extends BaseMapper<SysMessage> {
}
