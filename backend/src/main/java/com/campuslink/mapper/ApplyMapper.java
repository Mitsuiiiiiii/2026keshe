package com.campuslink.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.campuslink.dto.ApplyVO;
import com.campuslink.entity.Apply;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 申请加入 Mapper。
 */
@Mapper
public interface ApplyMapper extends BaseMapper<Apply> {

    /** 某队伍收到的申请（含申请人信息），供队长审核 */
    @Select("""
            SELECT a.id, a.team_id AS teamId, a.user_id AS userId,
                   u.nickname, u.avatar, u.college, u.major, u.grade, u.reputation,
                   a.self_intro AS selfIntro, a.skill_desc AS skillDesc, a.profile_link AS profileLink,
                   a.status, a.reject_reason AS rejectReason, a.create_time AS createTime
            FROM apply a JOIN user u ON u.id = a.user_id
            WHERE a.team_id = #{teamId}
            ORDER BY a.create_time DESC
            """)
    List<ApplyVO> listByTeam(@Param("teamId") Long teamId);

    /** 我提交的申请（含队伍名） */
    @Select("""
            SELECT a.id, a.team_id AS teamId, t.name AS teamName, a.user_id AS userId,
                   a.self_intro AS selfIntro, a.skill_desc AS skillDesc, a.profile_link AS profileLink,
                   a.status, a.reject_reason AS rejectReason, a.create_time AS createTime
            FROM apply a JOIN team t ON t.id = a.team_id
            WHERE a.user_id = #{userId}
            ORDER BY a.create_time DESC
            """)
    List<ApplyVO> listByUser(@Param("userId") Long userId);
}
