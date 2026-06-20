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

    /** 队伍任务列表（含负责人昵称），按状态与排序 */
    @Select("""
            SELECT t.id, t.team_id AS teamId, t.title, t.description,
                   t.assignee_id AS assigneeId, u.nickname AS assigneeName,
                   t.deadline, t.status, t.sort_order AS sortOrder, t.create_time AS createTime
            FROM task t LEFT JOIN user u ON u.id = t.assignee_id
            WHERE t.team_id = #{teamId}
            ORDER BY t.sort_order ASC, t.create_time ASC
            """)
    List<TaskVO> listByTeam(@Param("teamId") Long teamId);
}
