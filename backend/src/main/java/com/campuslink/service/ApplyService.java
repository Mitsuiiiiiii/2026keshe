package com.campuslink.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.campuslink.common.BusinessException;
import com.campuslink.common.ResultCode;
import com.campuslink.dto.ApplyDTO;
import com.campuslink.dto.ApplyVO;
import com.campuslink.dto.AuditDTO;
import com.campuslink.entity.Apply;
import com.campuslink.entity.Team;
import com.campuslink.entity.TeamMember;
import com.campuslink.entity.User;
import com.campuslink.mapper.ApplyMapper;
import com.campuslink.mapper.TeamMapper;
import com.campuslink.mapper.TeamMemberMapper;
import com.campuslink.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 申请加入服务：提交、审核、状态流转，并联动队伍成员与站内消息。
 */
@Service
@RequiredArgsConstructor
public class ApplyService {

    private final ApplyMapper applyMapper;
    private final TeamMapper teamMapper;
    private final TeamMemberMapper teamMemberMapper;
    private final UserMapper userMapper;
    private final MessageService messageService;

    /** 学生提交申请 */
    @Transactional(rollbackFor = Exception.class)
    public void apply(ApplyDTO dto, Long userId) {
        Team team = teamMapper.selectById(dto.getTeamId());
        if (team == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "队伍不存在");
        }
        if (!"RECRUITING".equals(team.getStatus())) {
            throw new BusinessException("该队伍已停止招募");
        }
        if (team.getLeaderId().equals(userId)) {
            throw new BusinessException("你是该队伍队长，无需申请");
        }
        // 已是成员
        Long isMember = teamMemberMapper.selectCount(new LambdaQueryWrapper<TeamMember>()
                .eq(TeamMember::getTeamId, dto.getTeamId())
                .eq(TeamMember::getUserId, userId));
        if (isMember != null && isMember > 0) {
            throw new BusinessException("你已经是该队伍成员");
        }
        // 已有待审核申请
        Long pending = applyMapper.selectCount(new LambdaQueryWrapper<Apply>()
                .eq(Apply::getTeamId, dto.getTeamId())
                .eq(Apply::getUserId, userId)
                .eq(Apply::getStatus, "PENDING"));
        if (pending != null && pending > 0) {
            throw new BusinessException("你已提交申请，请耐心等待审核");
        }

        Apply apply = new Apply();
        apply.setTeamId(dto.getTeamId());
        apply.setUserId(userId);
        apply.setSelfIntro(dto.getSelfIntro());
        apply.setSkillDesc(dto.getSkillDesc());
        apply.setProfileLink(dto.getProfileLink());
        apply.setStatus("PENDING");
        applyMapper.insert(apply);

        // 通知队长
        User applicant = userMapper.selectById(userId);
        String nick = applicant != null ? applicant.getNickname() : "有人";
        messageService.create(team.getLeaderId(), "APPLY",
                String.format("%s 申请加入你的队伍「%s」", nick, team.getName()), apply.getId());
    }

    /** 队长查看本队申请 */
    public List<ApplyVO> listByTeam(Long teamId, Long operatorId) {
        Team team = teamMapper.selectById(teamId);
        if (team == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "队伍不存在");
        }
        if (!team.getLeaderId().equals(operatorId)) {
            throw new BusinessException(ResultCode.FORBIDDEN, "仅队长可查看申请");
        }
        return applyMapper.listByTeam(teamId);
    }

    /** 我提交的申请 */
    public List<ApplyVO> listMine(Long userId) {
        return applyMapper.listByUser(userId);
    }

    /** 队长审核申请 */
    @Transactional(rollbackFor = Exception.class)
    public void audit(Long applyId, AuditDTO dto, Long operatorId) {
        Apply apply = applyMapper.selectById(applyId);
        if (apply == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "申请不存在");
        }
        if (!"PENDING".equals(apply.getStatus())) {
            throw new BusinessException("该申请已处理");
        }
        Team team = teamMapper.selectById(apply.getTeamId());
        if (team == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "队伍不存在");
        }
        if (!team.getLeaderId().equals(operatorId)) {
            throw new BusinessException(ResultCode.FORBIDDEN, "仅队长可审核");
        }

        if (Boolean.TRUE.equals(dto.getApproved())) {
            if (team.getCurrentSize() >= team.getTotalSize()) {
                throw new BusinessException("队伍人数已满");
            }
            apply.setStatus("APPROVED");
            applyMapper.updateById(apply);

            // 加入成员
            TeamMember member = new TeamMember();
            member.setTeamId(team.getId());
            member.setUserId(apply.getUserId());
            member.setRole("MEMBER");
            teamMemberMapper.insert(member);

            // 更新人数与状态
            team.setCurrentSize(team.getCurrentSize() + 1);
            if (team.getCurrentSize() >= team.getTotalSize()) {
                team.setStatus("FULL");
            }
            teamMapper.updateById(team);

            messageService.create(apply.getUserId(), "AUDIT",
                    String.format("你加入队伍「%s」的申请已通过", team.getName()), team.getId());
        } else {
            apply.setStatus("REJECTED");
            apply.setRejectReason(dto.getReason());
            applyMapper.updateById(apply);

            String reason = dto.getReason() != null && !dto.getReason().isBlank()
                    ? "，理由：" + dto.getReason() : "";
            messageService.create(apply.getUserId(), "AUDIT",
                    String.format("你加入队伍「%s」的申请被拒绝%s", team.getName(), reason), team.getId());
        }
    }
}
