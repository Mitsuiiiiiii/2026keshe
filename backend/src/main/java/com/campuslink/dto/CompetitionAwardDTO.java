package com.campuslink.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 赛事获奖公示发布请求，v3 新增。
 *
 * @author liuguangyuan
 * @since 2026/6/29
 */
@Data
public class CompetitionAwardDTO {

    private Long teamId;

    @NotBlank(message = "队伍名称不能为空")
    private String teamName;

    /** 奖项等级：FIRST / SECOND / THIRD / EXCELLENT 等 */
    @NotBlank(message = "奖项等级不能为空")
    private String awardLevel;

    private Integer awardYear;

    private Integer memberCount;

    private String members;
}
