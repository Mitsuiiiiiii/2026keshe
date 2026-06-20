package com.campuslink.controller;

import com.campuslink.common.Result;
import com.campuslink.dto.UserUpdateDTO;
import com.campuslink.entity.Skill;
import com.campuslink.entity.User;
import com.campuslink.security.SecurityUtil;
import com.campuslink.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 用户接口：个人信息、技能标签管理。
 */
@RestController
@RequestMapping("/user")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    /** 当前登录用户信息 */
    @GetMapping("/me")
    public Result<User> me() {
        return Result.success(userService.getById(SecurityUtil.getUserId()));
    }

    /** 查看指定用户主页 */
    @GetMapping("/{id}")
    public Result<User> getUser(@PathVariable Long id) {
        return Result.success(userService.getById(id));
    }

    /** 更新当前用户资料 */
    @PutMapping("/me")
    public Result<User> updateProfile(@Valid @RequestBody UserUpdateDTO dto) {
        return Result.success(userService.updateProfile(SecurityUtil.getUserId(), dto));
    }

    /** 我的技能标签 */
    @GetMapping("/me/skills")
    public Result<List<Skill>> mySkills() {
        return Result.success(userService.listUserSkills(SecurityUtil.getUserId()));
    }

    /** 查看指定用户技能标签 */
    @GetMapping("/{id}/skills")
    public Result<List<Skill>> userSkills(@PathVariable Long id) {
        return Result.success(userService.listUserSkills(id));
    }

    /** 添加技能标签 */
    @PostMapping("/me/skills/{skillId}")
    public Result<Void> addSkill(@PathVariable Long skillId) {
        userService.addSkill(SecurityUtil.getUserId(), skillId);
        return Result.success();
    }

    /** 移除技能标签 */
    @DeleteMapping("/me/skills/{skillId}")
    public Result<Void> removeSkill(@PathVariable Long skillId) {
        userService.removeSkill(SecurityUtil.getUserId(), skillId);
        return Result.success();
    }
}
