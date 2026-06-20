package com.campuslink.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 申请视图：申请信息 + 申请人 / 队伍信息。
 */
@Data
public class ApplyVO {

    private Long id;

    private Long teamId;

    private String teamName;

    private Long userId;

    private String nickname;

    private String avatar;

    private String college;

    private String major;

    private Integer grade;

    private BigDecimal reputation;

    private String selfIntro;

    private String skillDesc;

    private String profileLink;

    private String status;

    private String rejectReason;

    private LocalDateTime createTime;
}
