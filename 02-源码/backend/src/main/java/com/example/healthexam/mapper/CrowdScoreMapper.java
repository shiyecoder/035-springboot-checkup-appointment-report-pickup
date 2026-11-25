package com.example.healthexam.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.healthexam.entity.CrowdScore;
import org.apache.ibatis.annotations.Mapper;

/** 拥挤度计算留痕 Mapper */
@Mapper
public interface CrowdScoreMapper extends BaseMapper<CrowdScore> {
}
