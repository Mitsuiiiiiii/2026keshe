package com.campuslink.controller;

import com.campuslink.common.Result;
import com.campuslink.entity.Skill;
import com.campuslink.service.SkillService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 技能库接口。
 */
@RestController
@RequestMapping("/skill")
@RequiredArgsConstructor
public class SkillController {

    private final SkillService skillService;

    @GetMapping
    public Result<List<Skill>> list(@RequestParam(required = false) String category) {
        return Result.success(skillService.list(category));
    }
}
