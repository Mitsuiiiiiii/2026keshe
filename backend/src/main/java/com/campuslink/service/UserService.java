package com.campuslink.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.campuslink.common.BusinessException;
import com.campuslink.common.ResultCode;
import com.campuslink.dto.UserUpdateDTO;
import com.campuslink.entity.Skill;
import com.campuslink.entity.User;
import com.campuslink.entity.UserSkill;
import com.campuslink.mapper.SkillMapper;
import com.campuslink.mapper.UserMapper;
import com.campuslink.mapper.UserSkillMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 用户信息与技能标签服务。
 */
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserMapper userMapper;
    private final SkillMapper skillMapper;
    private final UserSkillMapper userSkillMapper;

    public User getById(Long id) {
        User user = userMapper.selectById(id);
        if (user == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "用户不存在");
        }
        return user;
    }

    public User updateProfile(Long userId, UserUpdateDTO dto) {
        User user = getById(userId);
        if (dto.getNickname() != null) user.setNickname(dto.getNickname());
        if (dto.getEmail() != null) user.setEmail(dto.getEmail());
        if (dto.getAvatar() != null) user.setAvatar(dto.getAvatar());
        if (dto.getCollege() != null) user.setCollege(dto.getCollege());
        if (dto.getMajor() != null) user.setMajor(dto.getMajor());
        if (dto.getGrade() != null) user.setGrade(dto.getGrade());
        if (dto.getIntro() != null) user.setIntro(dto.getIntro());
        userMapper.updateById(user);
        return user;
    }

    public List<Skill> listUserSkills(Long userId) {
        return userSkillMapper.selectSkillsByUserId(userId);
    }

    public void addSkill(Long userId, Long skillId) {
        if (skillMapper.selectById(skillId) == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "技能不存在");
        }
        Long exists = userSkillMapper.selectCount(new LambdaQueryWrapper<UserSkill>()
                .eq(UserSkill::getUserId, userId)
                .eq(UserSkill::getSkillId, skillId));
        if (exists != null && exists > 0) {
            return; // 已存在，幂等处理
        }
        UserSkill us = new UserSkill();
        us.setUserId(userId);
        us.setSkillId(skillId);
        userSkillMapper.insert(us);
    }

    public void removeSkill(Long userId, Long skillId) {
        userSkillMapper.delete(new LambdaQueryWrapper<UserSkill>()
                .eq(UserSkill::getUserId, userId)
                .eq(UserSkill::getSkillId, skillId));
    }
}
