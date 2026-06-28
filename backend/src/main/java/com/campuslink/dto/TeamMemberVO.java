package com.campuslink.dto;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 队伍成员视图。
 */
@Data
public class TeamMemberVO {

    /** team_member 行 id，用于任命副队长等成员操作 */
    private Long memberId;

    private Long userId;

    private String nickname;

    private String avatar;

    private String major;

    private Integer grade;

    /** LEADER / MEMBER */
    private String role;

    private LocalDateTime joinTime;
}
