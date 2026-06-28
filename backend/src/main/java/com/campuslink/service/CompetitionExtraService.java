package com.campuslink.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.campuslink.common.BusinessException;
import com.campuslink.common.PageResult;
import com.campuslink.common.ResultCode;
import com.campuslink.dto.CompetitionAwardDTO;
import com.campuslink.dto.CompetitionScheduleDTO;
import com.campuslink.entity.Competition;
import com.campuslink.entity.CompetitionAttachment;
import com.campuslink.entity.CompetitionAward;
import com.campuslink.entity.CompetitionNews;
import com.campuslink.entity.CompetitionRegister;
import com.campuslink.entity.CompetitionSchedule;
import com.campuslink.entity.Team;
import com.campuslink.mapper.CompetitionAttachmentMapper;
import com.campuslink.mapper.CompetitionAwardMapper;
import com.campuslink.mapper.CompetitionMapper;
import com.campuslink.mapper.CompetitionNewsMapper;
import com.campuslink.mapper.CompetitionRegisterMapper;
import com.campuslink.mapper.CompetitionScheduleMapper;
import com.campuslink.mapper.TeamMapper;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 赛事拓展业务（报名 / 附件 / 资讯 / 排行榜），v2 新增。
 *
 * @author liuguangyuan
 * @since 2026/6/27
 */
@Service
@RequiredArgsConstructor
public class CompetitionExtraService {

    private static final Logger LOGGER = LoggerFactory.getLogger(CompetitionExtraService.class);

    private final CompetitionRegisterMapper registerMapper;
    private final CompetitionAttachmentMapper attachmentMapper;
    private final CompetitionNewsMapper newsMapper;
    private final CompetitionMapper competitionMapper;
    private final CompetitionScheduleMapper scheduleMapper;
    private final CompetitionAwardMapper awardMapper;
    private final TeamMapper teamMapper;
    private final MessageService messageService;
    private final MessageExtraService messageExtraService;

    /**
     * 队伍报名竞赛。
     */
    public CompetitionRegister register(Long competitionId, Long teamId, Long applicantId, String remark) {
        //1 校验竞赛
        Competition competition = competitionMapper.selectById(competitionId);
        if (competition == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "竞赛不存在");
        }
        if (competition.getDeadline() != null && competition.getDeadline().isBefore(LocalDateTime.now())) {
            LOGGER.warn("报名失败：竞赛已截止, competitionId={}", competitionId);
            throw new BusinessException(ResultCode.BAD_REQUEST, "竞赛报名已截止");
        }

        //2 校验队伍归属（队长才能报名）
        Team team = teamMapper.selectById(teamId);
        if (team == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "队伍不存在");
        }
        if (!team.getLeaderId().equals(applicantId)) {
            LOGGER.warn("报名失败：非队长操作, teamId={}, applicantId={}", teamId, applicantId);
            throw new BusinessException(ResultCode.FORBIDDEN, "仅队长可代表队伍报名");
        }
        if ("ARCHIVED".equals(team.getStatus())) {
            throw new BusinessException(ResultCode.TEAM_ARCHIVED);
        }

        //3 防重
        Long exists = registerMapper.selectCount(new LambdaQueryWrapper<CompetitionRegister>()
                .eq(CompetitionRegister::getCompetitionId, competitionId)
                .eq(CompetitionRegister::getTeamId, teamId));
        if (exists != null && exists > 0) {
            throw new BusinessException(ResultCode.DUPLICATE_RECORD, "队伍已经报名过该竞赛");
        }

        //4 写报名记录
        CompetitionRegister entity = new CompetitionRegister();
        entity.setCompetitionId(competitionId);
        entity.setTeamId(teamId);
        entity.setApplicantId(applicantId);
        entity.setRemark(remark);
        entity.setStatus("PENDING");
        registerMapper.insert(entity);
        LOGGER.info("队伍报名完成, registerId={}, competitionId={}, teamId={}", entity.getId(), competitionId, teamId);

        //5 推送站内消息给队长（仅本人，便于在「我的消息」可见）
        messageService.create(applicantId, "AUDIT",
                "已为队伍【" + team.getName() + "】提交赛事【" + competition.getName() + "】报名，等待管理员审核",
                entity.getId());
        return entity;
    }

    /**
     * 审核报名（管理员）。
     */
    public CompetitionRegister audit(Long registerId, boolean approved, String reason) {
        CompetitionRegister register = registerMapper.selectById(registerId);
        if (register == null) {
            throw new BusinessException(ResultCode.NOT_FOUND);
        }
        if (!"PENDING".equals(register.getStatus())) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "报名状态不可审核");
        }
        register.setStatus(approved ? "APPROVED" : "REJECTED");
        register.setAuditReason(reason);
        register.setAuditTime(LocalDateTime.now());
        registerMapper.updateById(register);
        LOGGER.info("赛事报名审核完成, registerId={}, approved={}", registerId, approved);

        Team team = teamMapper.selectById(register.getTeamId());
        if (team != null) {
            messageService.create(team.getLeaderId(), "AUDIT",
                    (approved ? "审核通过：" : "审核拒绝：") + (reason == null ? "" : reason),
                    registerId);
        }
        return register;
    }

    /**
     * 报名列表（管理员 / 队长视角）。
     */
    public Map<String, Object> pageRegisters(Long competitionId, String status, long current, long size) {
        LambdaQueryWrapper<CompetitionRegister> wrapper = new LambdaQueryWrapper<CompetitionRegister>()
                .eq(CompetitionRegister::getCompetitionId, competitionId)
                .orderByDesc(CompetitionRegister::getCreateTime);
        if (status != null && !status.isBlank()) {
            wrapper.eq(CompetitionRegister::getStatus, status);
        }
        Page<CompetitionRegister> page = registerMapper.selectPage(new Page<>(current, size), wrapper);
        return PageResult.wrap(page);
    }

    /**
     * 赛事附件列表。
     */
    public List<CompetitionAttachment> listAttachments(Long competitionId) {
        return attachmentMapper.selectList(new LambdaQueryWrapper<CompetitionAttachment>()
                .eq(CompetitionAttachment::getCompetitionId, competitionId)
                .orderByDesc(CompetitionAttachment::getCreateTime));
    }

    public CompetitionAttachment addAttachment(Long competitionId, Long fileId, String name, String category,
                                               Long currentUserId, boolean admin) {
        Competition competition = competitionMapper.selectById(competitionId);
        if (competition == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "竞赛不存在");
        }
        //权限校验：仅管理员或竞赛创建者可上传附件
        boolean isCreator = competition.getCreatorId() != null && competition.getCreatorId().equals(currentUserId);
        if (!admin && !isCreator) {
            LOGGER.warn("附件上传权限不足, competitionId={}, userId={}", competitionId, currentUserId);
            throw new BusinessException(ResultCode.FORBIDDEN, "仅管理员或竞赛创建者可上传附件");
        }
        CompetitionAttachment attachment = new CompetitionAttachment();
        attachment.setCompetitionId(competitionId);
        attachment.setFileId(fileId);
        attachment.setName(name);
        attachment.setCategory(category == null ? "OTHER" : category);
        attachmentMapper.insert(attachment);
        LOGGER.info("赛事附件已上传, attachmentId={}, competitionId={}", attachment.getId(), competitionId);
        return attachment;
    }

    public void deleteAttachment(Long attachmentId) {
        attachmentMapper.deleteById(attachmentId);
        LOGGER.info("赛事附件已删除, attachmentId={}", attachmentId);
    }

    /**
     * 资讯。
     */
    public List<CompetitionNews> listNews(Long competitionId) {
        return newsMapper.selectList(new LambdaQueryWrapper<CompetitionNews>()
                .eq(CompetitionNews::getCompetitionId, competitionId)
                .orderByDesc(CompetitionNews::getCreateTime));
    }

    public CompetitionNews publishNews(Long competitionId, String title, String content, Long authorId) {
        if (competitionMapper.selectById(competitionId) == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "竞赛不存在");
        }
        CompetitionNews news = new CompetitionNews();
        news.setCompetitionId(competitionId);
        news.setTitle(title);
        news.setContent(content);
        news.setAuthorId(authorId);
        newsMapper.insert(news);
        LOGGER.info("赛事资讯已发布, newsId={}, competitionId={}", news.getId(), competitionId);
        return news;
    }

    /**
     * 赛事排行榜（统计每个竞赛累计 APPROVED 报名数）。
     */
    public List<Map<String, Object>> ranking() {
        List<Competition> competitions = competitionMapper.selectList(null);
        java.util.List<Map<String, Object>> result = new java.util.ArrayList<>();
        for (Competition comp : competitions) {
            Long approvedCount = registerMapper.selectCount(new LambdaQueryWrapper<CompetitionRegister>()
                    .eq(CompetitionRegister::getCompetitionId, comp.getId())
                    .eq(CompetitionRegister::getStatus, "APPROVED"));
            Map<String, Object> item = new HashMap<>();
            item.put("competitionId", comp.getId());
            item.put("competitionName", comp.getName());
            item.put("type", comp.getType());
            item.put("approvedCount", approvedCount == null ? 0 : approvedCount);
            result.add(item);
        }
        result.sort((a, b) -> Long.compare(((Number) b.get("approvedCount")).longValue(),
                ((Number) a.get("approvedCount")).longValue()));
        return result;
    }

    // ===================== 赛事日程时间表 =====================

    /**
     * 发布赛事日程（管理员 / 竞赛创建者）。
     */
    public CompetitionSchedule addSchedule(Long competitionId, CompetitionScheduleDTO dto,
                                           Long currentUserId, boolean admin) {
        Competition competition = competitionMapper.selectById(competitionId);
        if (competition == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "竞赛不存在");
        }
        boolean isCreator = competition.getCreatorId() != null && competition.getCreatorId().equals(currentUserId);
        if (!admin && !isCreator) {
            LOGGER.warn("日程发布权限不足, competitionId={}, userId={}", competitionId, currentUserId);
            throw new BusinessException(ResultCode.FORBIDDEN, "仅管理员或竞赛创建者可发布日程");
        }
        CompetitionSchedule schedule = new CompetitionSchedule();
        schedule.setCompetitionId(competitionId);
        schedule.setStage(dto.getStage());
        schedule.setTitle(dto.getTitle());
        schedule.setStartTime(dto.getStartTime());
        schedule.setEndTime(dto.getEndTime());
        schedule.setLocation(dto.getLocation());
        schedule.setRemark(dto.getRemark());
        schedule.setSortOrder(dto.getSortOrder() == null ? 0 : dto.getSortOrder());
        scheduleMapper.insert(schedule);
        LOGGER.info("赛事日程已发布, scheduleId={}, competitionId={}", schedule.getId(), competitionId);
        return schedule;
    }

    /**
     * 赛事日程列表（按 sortOrder、开始时间排序）。
     */
    public List<CompetitionSchedule> listSchedules(Long competitionId) {
        return scheduleMapper.selectList(new LambdaQueryWrapper<CompetitionSchedule>()
                .eq(CompetitionSchedule::getCompetitionId, competitionId)
                .orderByAsc(CompetitionSchedule::getSortOrder)
                .orderByAsc(CompetitionSchedule::getStartTime));
    }

    /**
     * 删除赛事日程（管理员 / 竞赛创建者）。
     */
    public void deleteSchedule(Long scheduleId, Long currentUserId, boolean admin) {
        CompetitionSchedule schedule = scheduleMapper.selectById(scheduleId);
        if (schedule == null) {
            return;
        }
        Competition competition = competitionMapper.selectById(schedule.getCompetitionId());
        boolean isCreator = competition != null && competition.getCreatorId() != null
                && competition.getCreatorId().equals(currentUserId);
        if (!admin && !isCreator) {
            LOGGER.warn("日程删除权限不足, scheduleId={}, userId={}", scheduleId, currentUserId);
            throw new BusinessException(ResultCode.FORBIDDEN, "仅管理员或竞赛创建者可删除日程");
        }
        scheduleMapper.deleteById(scheduleId);
        LOGGER.info("赛事日程已删除, scheduleId={}", scheduleId);
    }

    // ===================== 获奖公示 =====================

    /**
     * 发布获奖公示（管理员）。成功后通知收藏者。
     */
    public CompetitionAward publishAward(Long competitionId, CompetitionAwardDTO dto, Long publisherId) {
        if (competitionMapper.selectById(competitionId) == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "竞赛不存在");
        }
        CompetitionAward award = new CompetitionAward();
        award.setCompetitionId(competitionId);
        award.setTeamId(dto.getTeamId());
        award.setTeamName(dto.getTeamName());
        award.setAwardLevel(dto.getAwardLevel());
        award.setAwardYear(dto.getAwardYear() == null ? LocalDateTime.now().getYear() : dto.getAwardYear());
        award.setMemberCount(dto.getMemberCount());
        award.setMembers(dto.getMembers());
        award.setPublisherId(publisherId);
        awardMapper.insert(award);
        LOGGER.info("赛事获奖公示已发布, awardId={}, competitionId={}", award.getId(), competitionId);

        //通知收藏该竞赛的用户（对外契约：E 模块 MessageExtraService.notifyFavoriters）
        try {
            messageExtraService.notifyFavoriters("COMPETITION", competitionId, "你收藏的竞赛有新获奖公示");
        } catch (Exception e) {
            LOGGER.warn("通知收藏者失败, competitionId={}", competitionId, e);
        }
        return award;
    }

    /**
     * 按竞赛列出获奖公示。
     */
    public List<CompetitionAward> listAwards(Long competitionId) {
        return awardMapper.selectList(new LambdaQueryWrapper<CompetitionAward>()
                .eq(CompetitionAward::getCompetitionId, competitionId)
                .orderByDesc(CompetitionAward::getAwardYear)
                .orderByAsc(CompetitionAward::getAwardLevel));
    }

    /**
     * 获奖排行榜：按队伍聚合历年获奖数 / 获奖人数，倒序返回榜单。
     */
    public List<Map<String, Object>> awardRanking() {
        List<CompetitionAward> awards = awardMapper.selectList(null);
        //以 teamId（无则用 teamName）为聚合键
        Map<String, Map<String, Object>> agg = new LinkedHashMap<>();
        for (CompetitionAward award : awards) {
            String key = award.getTeamId() != null ? "T" + award.getTeamId() : "N" + award.getTeamName();
            Map<String, Object> item = agg.computeIfAbsent(key, k -> {
                Map<String, Object> m = new HashMap<>();
                m.put("teamId", award.getTeamId());
                m.put("teamName", award.getTeamName());
                m.put("awardCount", 0L);
                m.put("memberCount", 0L);
                return m;
            });
            item.put("awardCount", ((Number) item.get("awardCount")).longValue() + 1);
            if (award.getMemberCount() != null) {
                item.put("memberCount", ((Number) item.get("memberCount")).longValue() + award.getMemberCount());
            }
        }
        List<Map<String, Object>> result = new ArrayList<>(agg.values());
        result.sort((a, b) -> Long.compare(((Number) b.get("awardCount")).longValue(),
                ((Number) a.get("awardCount")).longValue()));
        return result;
    }

    /**
     * 获奖统计：总获奖队伍数、总获奖人数，并按年份 / 奖项分组统计。
     */
    public Map<String, Object> awardStats() {
        List<CompetitionAward> awards = awardMapper.selectList(null);
        long totalTeams = awards.size();//每条记录视为一支获奖队伍
        long totalMembers = 0L;
        Map<Integer, Long> byYear = new LinkedHashMap<>();
        Map<String, Long> byLevel = new LinkedHashMap<>();
        for (CompetitionAward award : awards) {
            if (award.getMemberCount() != null) {
                totalMembers += award.getMemberCount();
            }
            Integer year = award.getAwardYear();
            if (year != null) {
                byYear.merge(year, 1L, Long::sum);
            }
            String level = award.getAwardLevel();
            if (level != null) {
                byLevel.merge(level, 1L, Long::sum);
            }
        }
        Map<String, Object> result = new HashMap<>();
        result.put("totalTeams", totalTeams);
        result.put("totalMembers", totalMembers);
        result.put("byYear", byYear);
        result.put("byLevel", byLevel);
        return result;
    }

    // ===================== 我的报名 =====================

    /**
     * 我的报名：当前用户作为队长的所有队伍的竞赛报名记录及状态。
     */
    public List<Map<String, Object>> myRegisters(Long userId) {
        //1 查我作为队长的队伍
        List<Team> teams = teamMapper.selectList(new LambdaQueryWrapper<Team>()
                .eq(Team::getLeaderId, userId));
        List<Map<String, Object>> result = new ArrayList<>();
        if (teams.isEmpty()) {
            return result;
        }
        List<Long> teamIds = new ArrayList<>();
        Map<Long, Team> teamMap = new HashMap<>();
        for (Team team : teams) {
            teamIds.add(team.getId());
            teamMap.put(team.getId(), team);
        }
        //2 查这些队伍的报名记录
        List<CompetitionRegister> registers = registerMapper.selectList(
                new LambdaQueryWrapper<CompetitionRegister>()
                        .in(CompetitionRegister::getTeamId, teamIds)
                        .orderByDesc(CompetitionRegister::getCreateTime));
        for (CompetitionRegister register : registers) {
            Competition competition = competitionMapper.selectById(register.getCompetitionId());
            Team team = teamMap.get(register.getTeamId());
            Map<String, Object> item = new HashMap<>();
            item.put("registerId", register.getId());
            item.put("competitionId", register.getCompetitionId());
            item.put("competitionName", competition == null ? null : competition.getName());
            item.put("teamId", register.getTeamId());
            item.put("teamName", team == null ? null : team.getName());
            item.put("status", register.getStatus());
            item.put("auditReason", register.getAuditReason());
            item.put("createTime", register.getCreateTime());
            result.add(item);
        }
        return result;
    }

    // ===================== 报名截止自动拦截（供定时任务调用） =====================

    /**
     * 关闭已过 deadline 竞赛的报名通道：将其下 PENDING 报名记录自动置为 REJECTED。
     *
     * @return 实际关闭的报名记录数
     */
    public int closeExpiredRegisterChannels() {
        LocalDateTime now = LocalDateTime.now();
        List<Competition> expired = competitionMapper.selectList(new LambdaQueryWrapper<Competition>()
                .lt(Competition::getDeadline, now)
                .isNotNull(Competition::getDeadline));
        if (expired.isEmpty()) {
            return 0;
        }
        int closed = 0;
        for (Competition competition : expired) {
            List<CompetitionRegister> pendings = registerMapper.selectList(
                    new LambdaQueryWrapper<CompetitionRegister>()
                            .eq(CompetitionRegister::getCompetitionId, competition.getId())
                            .eq(CompetitionRegister::getStatus, "PENDING"));
            for (CompetitionRegister register : pendings) {
                register.setStatus("REJECTED");
                register.setAuditReason("报名通道已截止，系统自动关闭");
                register.setAuditTime(now);
                registerMapper.updateById(register);
                closed++;
                Team team = teamMapper.selectById(register.getTeamId());
                if (team != null) {
                    messageService.create(team.getLeaderId(), "AUDIT",
                            "竞赛【" + competition.getName() + "】报名已截止，您的待审报名已自动关闭",
                            register.getId());
                }
            }
        }
        return closed;
    }
}
