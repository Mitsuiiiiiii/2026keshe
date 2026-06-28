package com.campuslink.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.campuslink.common.BusinessException;
import com.campuslink.common.ResultCode;
import com.campuslink.dto.TaskDTO;
import com.campuslink.dto.TaskStatVO;
import com.campuslink.dto.TaskStatusDTO;
import com.campuslink.dto.TaskVO;
import com.campuslink.entity.Task;
import com.campuslink.entity.Team;
import com.campuslink.entity.TeamMember;
import com.campuslink.mapper.TaskMapper;
import com.campuslink.mapper.TeamMapper;
import com.campuslink.mapper.TeamMemberMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;

/**
 * 队内协作任务服务：任务 CRUD、看板状态更新、进度统计。
 */
@Service
@RequiredArgsConstructor
public class TaskService {

    private static final Set<String> STATUSES = Set.of("TODO", "DOING", "DONE");

    private final TaskMapper taskMapper;
    private final TeamMapper teamMapper;
    private final TeamMemberMapper teamMemberMapper;
    private final MessageService messageService;

    public List<TaskVO> listByTeam(Long teamId, Long operatorId) {
        requireMember(teamId, operatorId);
        return taskMapper.listByTeam(teamId);
    }

    public Task create(TaskDTO dto, Long operatorId) {
        if (dto.getTeamId() == null) {
            throw new BusinessException("队伍 id 不能为空");
        }
        requireMember(dto.getTeamId(), operatorId);

        Task task = new Task();
        task.setTeamId(dto.getTeamId());
        task.setTitle(dto.getTitle());
        task.setDescription(dto.getDescription());
        task.setAssigneeId(dto.getAssigneeId());
        task.setDeadline(dto.getDeadline());
        task.setStatus("TODO");
        task.setPriority(dto.getPriority() == null || dto.getPriority().isBlank() ? "MEDIUM" : dto.getPriority());
        task.setTags(dto.getTags());
        task.setSortOrder(0);
        taskMapper.insert(task);

        notifyAssignee(task, operatorId);
        return task;
    }

    public Task update(Long id, TaskDTO dto, Long operatorId) {
        Task task = taskMapper.selectById(id);
        if (task == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "任务不存在");
        }
        requireMember(task.getTeamId(), operatorId);

        Long oldAssignee = task.getAssigneeId();
        if (dto.getTitle() != null) task.setTitle(dto.getTitle());
        if (dto.getDescription() != null) task.setDescription(dto.getDescription());
        if (dto.getDeadline() != null) task.setDeadline(dto.getDeadline());
        if (dto.getPriority() != null && !dto.getPriority().isBlank()) task.setPriority(dto.getPriority());
        if (dto.getTags() != null) task.setTags(dto.getTags());
        task.setAssigneeId(dto.getAssigneeId());
        taskMapper.updateById(task);

        // 负责人变更则通知新负责人
        if (dto.getAssigneeId() != null && !dto.getAssigneeId().equals(oldAssignee)) {
            notifyAssignee(task, operatorId);
        }
        return task;
    }

    public Task updateStatus(Long id, TaskStatusDTO dto, Long operatorId) {
        if (!STATUSES.contains(dto.getStatus())) {
            throw new BusinessException("非法的任务状态");
        }
        Task task = taskMapper.selectById(id);
        if (task == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "任务不存在");
        }
        requireMember(task.getTeamId(), operatorId);
        task.setStatus(dto.getStatus());
        if (dto.getSortOrder() != null) {
            task.setSortOrder(dto.getSortOrder());
        }
        taskMapper.updateById(task);
        return task;
    }

    public void delete(Long id, Long operatorId) {
        Task task = taskMapper.selectById(id);
        if (task == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "任务不存在");
        }
        requireMember(task.getTeamId(), operatorId);
        taskMapper.deleteById(id);
    }

    public TaskStatVO stat(Long teamId, Long operatorId) {
        requireMember(teamId, operatorId);
        List<Task> tasks = taskMapper.selectList(
                new LambdaQueryWrapper<Task>().eq(Task::getTeamId, teamId).isNull(Task::getParentId));
        TaskStatVO vo = new TaskStatVO();
        vo.setTodo(tasks.stream().filter(t -> "TODO".equals(t.getStatus())).count());
        vo.setDoing(tasks.stream().filter(t -> "DOING".equals(t.getStatus())).count());
        vo.setDone(tasks.stream().filter(t -> "DONE".equals(t.getStatus())).count());
        vo.setTotal(tasks.size());
        return vo;
    }

    private void notifyAssignee(Task task, Long operatorId) {
        if (task.getAssigneeId() == null || task.getAssigneeId().equals(operatorId)) {
            return;
        }
        Team team = teamMapper.selectById(task.getTeamId());
        String teamName = team != null ? team.getName() : "";
        messageService.create(task.getAssigneeId(), "TASK",
                String.format("你被指派了队伍「%s」的任务：%s", teamName, task.getTitle()), task.getId());
    }

    private void requireMember(Long teamId, Long userId) {
        Long count = teamMemberMapper.selectCount(new LambdaQueryWrapper<TeamMember>()
                .eq(TeamMember::getTeamId, teamId)
                .eq(TeamMember::getUserId, userId));
        if (count == null || count == 0) {
            throw new BusinessException(ResultCode.FORBIDDEN, "你不是该队伍成员");
        }
    }
}
