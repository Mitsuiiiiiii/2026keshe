package com.campuslink.controller;

import com.campuslink.common.Result;
import com.campuslink.dto.LoginDTO;
import com.campuslink.dto.LoginVO;
import com.campuslink.dto.RegisterDTO;
import com.campuslink.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 认证接口：注册、登录、退出。
 */
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    public Result<Void> register(@Valid @RequestBody RegisterDTO dto) {
        authService.register(dto);
        return Result.success();
    }

    @PostMapping("/login")
    public Result<LoginVO> login(@Valid @RequestBody LoginDTO dto) {
        return Result.success(authService.login(dto));
    }

    @PostMapping("/logout")
    public Result<Void> logout() {
        // 无状态 JWT，前端丢弃 token 即可；预留接口（后续可接入 Redis 黑名单）
        return Result.success();
    }
}
