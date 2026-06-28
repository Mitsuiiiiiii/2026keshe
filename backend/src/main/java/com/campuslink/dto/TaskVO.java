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

    /** LOW / MEDIUM / HIGH，v2 任务优先级 */
    private String priority;

    /** 逗号分隔标签，v2 */
    private String tags;

    /** 父任务 id，主任务为 null */
    private Long parentId;

    private Integer sortOrder;

    private LocalDateTime createTime;
}
