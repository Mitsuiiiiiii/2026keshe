package com.campuslink.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 任务新增 / 编辑请求。
 */
@Data
public class TaskDTO {

    /** 新增时必填，编辑时忽略 */
    private Long teamId;

    @NotBlank(message = "任务标题不能为空")
    private String title;

    private String description;

    private Long assigneeId;

    private LocalDateTime deadline;

    /** LOW / MEDIUM / HIGH，留空默认 MEDIUM */
    private String priority;

    /** 逗号分隔标签 */
    private String tags;
}
