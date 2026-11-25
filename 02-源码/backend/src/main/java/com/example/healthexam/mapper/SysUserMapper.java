package com.example.healthexam.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.healthexam.entity.SysUser;
import org.apache.ibatis.annotations.Mapper;

/** 用户表 Mapper */
@Mapper
public interface SysUserMapper extends BaseMapper<SysUser> {
}
