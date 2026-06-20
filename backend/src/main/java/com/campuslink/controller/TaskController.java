package com.campuslink.controller;

import com.campuslink.common.Result;
import com.campuslink.dto.TaskDTO;
import com.campuslink.dto.TaskStatVO;
import com.campuslink.dto.TaskStatusDTO;
import com.campuslink.dto.TaskVO;
import com.campuslink.entity.Task;
import com.campuslink.security.SecurityUtil;
import com.campuslink.service.TaskService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 队内协作任务接口：看板列表、CRUD、状态更新、进度统计。
 */
@RestController
@RequestMapping("/task")
@RequiredArgsConstructor
public class TaskController {

    private final TaskService taskService;

    @GetMapping("/team/{teamId}")
    public Result<List<TaskVO>> listByTeam(@PathVariable Long teamId) {
        return Result.success(taskService.listByTeam(teamId, SecurityUtil.getUserId()));
    }

    @GetMapping("/team/{teamId}/stat")
    public Result<TaskStatVO> stat(@PathVariable Long teamId) {
        return Result.success(taskService.stat(teamId, SecurityUtil.getUserId()));
    }

    @PostMapping
    public Result<Task> create(@Valid @RequestBody TaskDTO dto) {
        return Result.success(taskService.create(dto, SecurityUtil.getUserId()));
    }

    @PutMapping("/{id}")
    public Result<Task> update(@PathVariable Long id, @Valid @RequestBody TaskDTO dto) {
        return Result.success(taskService.update(id, dto, SecurityUtil.getUserId()));
    }

    @PutMapping("/{id}/status")
    public Result<Task> updateStatus(@PathVariable Long id, @Valid @RequestBody TaskStatusDTO dto) {
        return Result.success(taskService.updateStatus(id, dto, SecurityUtil.getUserId()));
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        taskService.delete(id, SecurityUtil.getUserId());
        return Result.success();
    }
}
