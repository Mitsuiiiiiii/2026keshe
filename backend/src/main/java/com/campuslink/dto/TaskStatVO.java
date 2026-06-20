package com.campuslink.dto;

import lombok.Data;

/**
 * 任务进度统计：各状态数量。
 */
@Data
public class TaskStatVO {

    private long todo;

    private long doing;

    private long done;

    private long total;
}
