package com.campuslink.schedule;

import com.campuslink.service.CompetitionExtraService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * 报名截止自动拦截定时任务，v3 新增。
 *
 * <p>策略：每半小时巡检一次，将已过 {@code deadline} 竞赛下仍处于 {@code PENDING}
 * 的报名记录自动置为 {@code REJECTED}（理由「报名通道已截止，系统自动关闭」），
 * 并给队长推送站内消息。新报名在 {@link CompetitionExtraService#register} 入口已按
 * deadline 拦截，本任务负责清理截止后遗留的待审报名，确保报名通道彻底关闭。
 *
 * @author liuguangyuan
 * @since 2026/6/29
 */
@Component
@RequiredArgsConstructor
public class CompetitionDeadlineScheduler {

    private static final Logger LOGGER = LoggerFactory.getLogger(CompetitionDeadlineScheduler.class);

    private final CompetitionExtraService competitionExtraService;

    /**
     * 报名截止巡检。
     */
    @Scheduled(cron = "${campuslink.schedule.competition-deadline-cron:0 0/30 * * * ?}")
    public void scanDeadline() {
        LocalDateTime now = LocalDateTime.now();
        LOGGER.info("[定时] 报名截止巡检启动, now={}", now);
        int closed = competitionExtraService.closeExpiredRegisterChannels();
        LOGGER.info("[定时] 报名截止巡检结束, 自动关闭待审报名数={}", closed);
    }
}
