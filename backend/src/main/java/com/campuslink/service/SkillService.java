package com.campuslink.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.campuslink.entity.Skill;
import com.campuslink.mapper.SkillMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * 技能库服务。
 */
@Service
@RequiredArgsConstructor
public class SkillService {

    private final SkillMapper skillMapper;

    public List<Skill> list(String category) {
        LambdaQueryWrapper<Skill> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(category)) {
            wrapper.eq(Skill::getCategory, category);
        }
        wrapper.orderByAsc(Skill::getId);
        return skillMapper.selectList(wrapper);
    }
}
