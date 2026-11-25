package com.example.healthexam.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.healthexam.entity.GroupMember;
import org.apache.ibatis.annotations.Mapper;

/** 团检人员名单 Mapper */
@Mapper
public interface GroupMemberMapper extends BaseMapper<GroupMember> {
}
