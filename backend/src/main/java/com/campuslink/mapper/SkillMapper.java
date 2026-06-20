package com.campuslink.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.campuslink.entity.Skill;
import org.apache.ibatis.annotations.Mapper;

/**
 * 技能库 Mapper。
 */
@Mapper
public interface SkillMapper extends BaseMapper<Skill> {
}
