package com.campuslink.dto;

import lombok.Data;

/**
 * 更新个人资料请求。
 */
@Data
public class UserUpdateDTO {

    private String nickname;

    private String email;

    private String avatar;

    private String college;

    private String major;

    private Integer grade;

    private String intro;
}
