package com.campuslink.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 赛事日程发布请求，v3 新增。
 *
 * @author liuguangyuan
 * @since 2026/6/29
 */
@Data
public class CompetitionScheduleDTO {

    private String stage;

    @NotBlank(message = "日程标题不能为空")
    private String title;

    private LocalDateTime startTime;

    private LocalDateTime endTime;

    private String location;

    private String remark;

    private Integer sortOrder;
}
