package com.campuslink.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.campuslink.dto.TaskVO;
import com.campuslink.entity.Task;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 任务 Mapper。
 */
@Mapper
public interface TaskMapper extends BaseMapper<Task> {

    /** 队伍主任务列表（含负责人昵称、优先级、标签），仅返回 parent_id 为空的主任务 */
    @Select("""
            SELECT t.id, t.team_id AS teamId, t.title, t.description,
                   t.assignee_id AS assigneeId, u.nickname AS assigneeName,
                   t.deadline, t.status, t.priority, t.tags, t.parent_id AS parentId,
                   t.sort_order AS sortOrder, t.create_time AS createTime
            FROM task t LEFT JOIN user u ON u.id = t.assignee_id
            WHERE t.team_id = #{teamId} AND t.parent_id IS NULL
            ORDER BY t.sort_order ASC, t.create_time ASC
            """)
    List<TaskVO> listByTeam(@Param("teamId") Long teamId);
}
