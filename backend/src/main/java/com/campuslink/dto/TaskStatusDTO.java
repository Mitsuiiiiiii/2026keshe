package com.campuslink.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 看板拖拽：更新任务状态与排序。
 */
@Data
public class TaskStatusDTO {

    @NotBlank(message = "状态不能为空")
    private String status;

    private Integer sortOrder;
}
