package com.campuslink.dto;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 任务视图：任务信息 + 负责人昵称。
 */
@Data
public class TaskVO {

    private Long id;

    private Long teamId;

    private String title;

    private String description;

    private Long assigneeId;

    private String assigneeName;

    private LocalDateTime deadline;

    private String status;

    private Integer sortOrder;

    private LocalDateTime createTime;
}
