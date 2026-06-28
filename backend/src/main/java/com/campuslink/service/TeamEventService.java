package com.campuslink.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.campuslink.entity.TeamEvent;
import com.campuslink.mapper.TeamEventMapper;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 队伍关键事件时间线业务（动态墙），v3 新增。
 * 对外提供 record 契约方法，供其它模块在关键操作后写入事件。
 *
 * @author liuguangyuan
 * @since 2026/6/29
 */
@Service
@RequiredArgsConstructor
public class TeamEventService {

    private static final Logger LOGGER = LoggerFactory.getLogger(TeamEventService.class);

    private final TeamEventMapper teamEventMapper;

    /**
     * 记录一条队伍事件，异常吞掉不影响主流程。
     *
     * @param teamId  队伍 id
     * @param type    事件类型
     * @param content 事件描述
     * @param actorId 触发人 id
     * @param refId   关联业务 id（可空）
     */
    public void record(Long teamId, String type, String content, Long actorId, Long refId) {
        try {
            TeamEvent event = new TeamEvent();
            event.setTeamId(teamId);
            event.setType(type);
            event.setContent(content);
            event.setActorId(actorId);
            event.setRefId(refId);
            teamEventMapper.insert(event);
        } catch (Exception e) {
            LOGGER.warn("队伍事件记录失败, teamId={}, type={}, err={}", teamId, type, e.getMessage());
        }
    }

    /** 队伍事件时间线，按时间倒序 */
    public List<TeamEvent> listByTeam(Long teamId) {
        return teamEventMapper.selectList(new LambdaQueryWrapper<TeamEvent>()
                .eq(TeamEvent::getTeamId, teamId)
                .orderByDesc(TeamEvent::getCreateTime));
    }
}
