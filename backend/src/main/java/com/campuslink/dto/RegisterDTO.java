package com.campuslink.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 注册请求。
 */
@Data
public class RegisterDTO {

    @NotBlank(message = "账号不能为空")
    @Size(min = 3, max = 50, message = "账号长度为 3-50")
    private String username;

    @NotBlank(message = "密码不能为空")
    @Size(min = 6, max = 50, message = "密码长度为 6-50")
    private String password;

    @NotBlank(message = "昵称不能为空")
    private String nickname;

    private String email;

    private String college;

    private String major;

    private Integer grade;
}
