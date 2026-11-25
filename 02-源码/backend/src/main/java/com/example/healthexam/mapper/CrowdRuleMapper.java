package com.example.healthexam.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.healthexam.entity.CrowdRule;
import org.apache.ibatis.annotations.Mapper;

/** 拥挤度规则版本 Mapper */
@Mapper
public interface CrowdRuleMapper extends BaseMapper<CrowdRule> {
}
