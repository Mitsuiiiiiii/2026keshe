package com.campuslink.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.campuslink.common.Result;
import com.campuslink.dto.CompetitionAwardDTO;
import com.campuslink.dto.CompetitionDTO;
import com.campuslink.dto.CompetitionScheduleDTO;
import com.campuslink.entity.Competition;
import com.campuslink.entity.CompetitionAttachment;
import com.campuslink.entity.CompetitionAward;
import com.campuslink.entity.CompetitionNews;
import com.campuslink.entity.CompetitionRegister;
import com.campuslink.entity.CompetitionSchedule;
import com.campuslink.security.SecurityUtil;
import com.campuslink.service.CompetitionExtraService;
import com.campuslink.service.CompetitionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 竞赛接口：v1 列表/CRUD + v2 报名/附件/资讯/排行榜。
 *
 * @author liuguangyuan
 * @since 2026/6/27
 */
@RestController
@RequestMapping("/competition")
@RequiredArgsConstructor
public class CompetitionController {

    private final CompetitionService competitionService;
    private final CompetitionExtraService competitionExtraService;

    @GetMapping
    public Result<IPage<Competition>> page(
            @RequestParam(defaultValue = "1") Integer current,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String keyword) {
        return Result.success(competitionService.page(current, size, type, keyword));
    }

    @GetMapping("/{id}")
    public Result<Competition> getById(@PathVariable Long id) {
        return Result.success(competitionService.getById(id));
    }

    @PostMapping
    public Result<Competition> create(@Valid @RequestBody CompetitionDTO dto) {
        return Result.success(competitionService.create(dto, SecurityUtil.getUserId()));
    }

    @PutMapping("/{id}")
    public Result<Competition> update(@PathVariable Long id, @Valid @RequestBody CompetitionDTO dto) {
        return Result.success(competitionService.update(id, dto));
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        competitionService.delete(id);
        return Result.success();
    }

    /**
     * 队伍报名竞赛。
     */
    @PostMapping("/{id}/register")
    public Result<CompetitionRegister> register(@PathVariable("id") Long competitionId,
                                                @RequestBody Map<String, Object> body) {
        Long teamId = Long.valueOf(body.get("teamId").toString());
        String remark = (String) body.getOrDefault("remark", "");
        return Result.success(competitionExtraService.register(competitionId, teamId, SecurityUtil.getUserId(), remark));
    }

    /**
     * 审核报名（管理员）。
     */
    @PutMapping("/register/{registerId}/audit")
    public Result<CompetitionRegister> audit(@PathVariable Long registerId,
                                             @RequestBody Map<String, Object> body) {
        Boolean approved = (Boolean) body.getOrDefault("approved", Boolean.FALSE);
        String reason = (String) body.getOrDefault("reason", "");
        return Result.success(competitionExtraService.audit(registerId, Boolean.TRUE.equals(approved), reason));
    }

    /**
     * 报名列表。
     */
    @GetMapping("/{id}/register")
    public Result<Map<String, Object>> listRegister(@PathVariable("id") Long competitionId,
                                                    @RequestParam(required = false) String status,
                                                    @RequestParam(defaultValue = "1") long current,
                                                    @RequestParam(defaultValue = "10") long size) {
        return Result.success(competitionExtraService.pageRegisters(competitionId, status, current, size));
    }

    /**
     * 赛事附件列表。
     */
    @GetMapping("/{id}/attachments")
    public Result<List<CompetitionAttachment>> attachments(@PathVariable("id") Long competitionId) {
        return Result.success(competitionExtraService.listAttachments(competitionId));
    }

    /**
     * 新增赛事附件。
     */
    @PostMapping("/{id}/attachments")
    public Result<CompetitionAttachment> addAttachment(@PathVariable("id") Long competitionId,
                                                       @RequestBody Map<String, Object> body) {
        Long fileId = Long.valueOf(body.get("fileId").toString());
        String name = (String) body.get("name");
        String category = (String) body.getOrDefault("category", "OTHER");
        return Result.success(competitionExtraService.addAttachment(competitionId, fileId, name, category,
                SecurityUtil.getUserId(), SecurityUtil.isAdmin()));
    }

    @DeleteMapping("/attachment/{attachmentId}")
    public Result<Void> deleteAttachment(@PathVariable Long attachmentId) {
        competitionExtraService.deleteAttachment(attachmentId);
        return Result.success();
    }

    /**
     * 赛事资讯。
     */
    @GetMapping("/{id}/news")
    public Result<List<CompetitionNews>> news(@PathVariable("id") Long competitionId) {
        return Result.success(competitionExtraService.listNews(competitionId));
    }

    @PostMapping("/{id}/news")
    public Result<CompetitionNews> publishNews(@PathVariable("id") Long competitionId,
                                               @RequestBody Map<String, Object> body) {
        String title = (String) body.get("title");
        String content = (String) body.get("content");
        return Result.success(competitionExtraService.publishNews(competitionId, title, content, SecurityUtil.getUserId()));
    }

    /**
     * 赛事排行榜（按 APPROVED 报名数）。
     */
    @GetMapping("/ranking")
    public Result<List<Map<String, Object>>> ranking() {
        return Result.success(competitionExtraService.ranking());
    }

    // ===================== 赛事日程时间表 =====================

    /**
     * 发布赛事日程（管理员 / 竞赛创建者）。
     */
    @PostMapping("/{id}/schedule")
    public Result<CompetitionSchedule> addSchedule(@PathVariable("id") Long competitionId,
                                                   @Valid @RequestBody CompetitionScheduleDTO dto) {
        return Result.success(competitionExtraService.addSchedule(competitionId, dto,
                SecurityUtil.getUserId(), SecurityUtil.isAdmin()));
    }

    /**
     * 赛事日程列表。
     */
    @GetMapping("/{id}/schedule")
    public Result<List<CompetitionSchedule>> listSchedule(@PathVariable("id") Long competitionId) {
        return Result.success(competitionExtraService.listSchedules(competitionId));
    }

    /**
     * 删除赛事日程（管理员 / 竞赛创建者）。
     */
    @DeleteMapping("/schedule/{sid}")
    public Result<Void> deleteSchedule(@PathVariable("sid") Long scheduleId) {
        competitionExtraService.deleteSchedule(scheduleId, SecurityUtil.getUserId(), SecurityUtil.isAdmin());
        return Result.success();
    }

    // ===================== 获奖公示 =====================

    /**
     * 发布获奖公示（管理员）。
     */
    @PostMapping("/{id}/awards")
    public Result<CompetitionAward> publishAward(@PathVariable("id") Long competitionId,
                                                 @Valid @RequestBody CompetitionAwardDTO dto) {
        if (!SecurityUtil.isAdmin()) {
            throw new com.campuslink.common.BusinessException(
                    com.campuslink.common.ResultCode.FORBIDDEN, "仅管理员可发布获奖公示");
        }
        return Result.success(competitionExtraService.publishAward(competitionId, dto, SecurityUtil.getUserId()));
    }

    /**
     * 按竞赛列出获奖公示。
     */
    @GetMapping("/{id}/awards")
    public Result<List<CompetitionAward>> listAwards(@PathVariable("id") Long competitionId) {
        return Result.success(competitionExtraService.listAwards(competitionId));
    }

    /**
     * 获奖排行榜（按队伍聚合历年获奖数 / 获奖人数）。
     */
    @GetMapping("/award/ranking")
    public Result<List<Map<String, Object>>> awardRanking() {
        return Result.success(competitionExtraService.awardRanking());
    }

    /**
     * 获奖统计（总队伍数 / 总人数 / 按年份、奖项分组）。
     */
    @GetMapping("/award/stats")
    public Result<Map<String, Object>> awardStats() {
        return Result.success(competitionExtraService.awardStats());
    }

    // ===================== 我的报名 =====================

    /**
     * 我的报名：当前用户作为队长的队伍的所有竞赛报名记录及状态。
     */
    @GetMapping("/my-registers")
    public Result<List<Map<String, Object>>> myRegisters() {
        return Result.success(competitionExtraService.myRegisters(SecurityUtil.getUserId()));
    }
}
