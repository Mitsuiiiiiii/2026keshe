package com.campuslink.dto;

import com.campuslink.entity.User;
import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * 登录响应：token + 用户信息。
 */
@Data
@AllArgsConstructor
public class LoginVO {

    private String token;

    private User user;
}
