package com.campuslink.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.campuslink.entity.Skill;
import com.campuslink.entity.UserSkill;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 用户技能关联 Mapper。
 */
@Mapper
public interface UserSkillMapper extends BaseMapper<UserSkill> {

    /**
     * 查询用户拥有的技能详情。
     */
    @Select("SELECT s.* FROM skill s JOIN user_skill us ON s.id = us.skill_id WHERE us.user_id = #{userId}")
    List<Skill> selectSkillsByUserId(Long userId);
}
