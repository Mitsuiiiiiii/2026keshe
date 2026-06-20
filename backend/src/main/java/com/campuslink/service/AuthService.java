package com.campuslink.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.campuslink.common.BusinessException;
import com.campuslink.dto.LoginDTO;
import com.campuslink.dto.LoginVO;
import com.campuslink.dto.RegisterDTO;
import com.campuslink.entity.User;
import com.campuslink.mapper.UserMapper;
import com.campuslink.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

/**
 * 注册 / 登录服务。
 */
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    public void register(RegisterDTO dto) {
        Long exists = userMapper.selectCount(
                new LambdaQueryWrapper<User>().eq(User::getUsername, dto.getUsername()));
        if (exists != null && exists > 0) {
            throw new BusinessException("账号已存在");
        }
        User user = new User();
        user.setUsername(dto.getUsername());
        user.setPassword(passwordEncoder.encode(dto.getPassword()));
        user.setNickname(dto.getNickname());
        user.setEmail(dto.getEmail());
        user.setCollege(dto.getCollege());
        user.setMajor(dto.getMajor());
        user.setGrade(dto.getGrade());
        user.setReputation(new BigDecimal("5.00"));
        user.setRole("STUDENT");
        userMapper.insert(user);
    }

    public LoginVO login(LoginDTO dto) {
        User user = userMapper.selectOne(
                new LambdaQueryWrapper<User>().eq(User::getUsername, dto.getUsername()));
        if (user == null || !passwordEncoder.matches(dto.getPassword(), user.getPassword())) {
            throw new BusinessException("账号或密码错误");
        }
        String token = jwtUtil.generateToken(user.getId(), user.getUsername(), user.getRole());
        return new LoginVO(token, user);
    }
}
