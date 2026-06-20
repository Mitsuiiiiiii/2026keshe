package com.campuslink.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 提交申请请求。
 */
@Data
public class ApplyDTO {

    @NotNull(message = "队伍 id 不能为空")
    private Long teamId;

    private String selfIntro;

    private String skillDesc;

    private String profileLink;
}
