package com.campuslink.service;

import com.campuslink.common.BusinessException;
import com.campuslink.entity.OperationLog;
import com.campuslink.entity.Report;
import com.campuslink.entity.SystemNotice;
import com.campuslink.entity.User;
import com.campuslink.mapper.ApplyMapper;
import com.campuslink.mapper.CompetitionMapper;
import com.campuslink.mapper.OperationLogMapper;
import com.campuslink.mapper.ReportMapper;
import com.campuslink.mapper.SkillMapper;
import com.campuslink.mapper.SystemNoticeMapper;
import com.campuslink.mapper.TaskMapper;
import com.campuslink.mapper.TeamMapper;
import com.campuslink.mapper.UserMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * {@link AdminService} 管理后台业务单元测试（Mockito，无 DB）。
 *
 * @author liuguangyuan
 * @since 2026/6/28
 */
class AdminServiceTest {

    private UserMapper userMapper;
    private TeamMapper teamMapper;
    private TaskMapper taskMapper;
    private ApplyMapper applyMapper;
    private CompetitionMapper competitionMapper;
    private ReportMapper reportMapper;
    private SkillMapper skillMapper;
    private SystemNoticeMapper systemNoticeMapper;
    private OperationLogMapper operationLogMapper;
    private PasswordEncoder passwordEncoder;
    private AdminService adminService;

    @BeforeEach
    void setUp() {
        userMapper = mock(UserMapper.class);
        teamMapper = mock(TeamMapper.class);
        taskMapper = mock(TaskMapper.class);
        applyMapper = mock(ApplyMapper.class);
        competitionMapper = mock(CompetitionMapper.class);
        reportMapper = mock(ReportMapper.class);
        skillMapper = mock(SkillMapper.class);
        systemNoticeMapper = mock(SystemNoticeMapper.class);
        operationLogMapper = mock(OperationLogMapper.class);
        passwordEncoder = mock(PasswordEncoder.class);
        adminService = new AdminService(userMapper, teamMapper, taskMapper, applyMapper,
                competitionMapper, reportMapper, skillMapper, systemNoticeMapper,
                operationLogMapper, passwordEncoder);
    }

    @Test
    @DisplayName("resetPassword 对存在用户加密并落库")
    void resetPasswordSuccess() {
        User user = new User();
        user.setId(7L);
        when(userMapper.selectById(7L)).thenReturn(user);
        when(passwordEncoder.encode("newpwd")).thenReturn("ENC");

        adminService.resetPassword(7L, "newpwd");

        assertEquals("ENC", user.getPassword());
        verify(userMapper).updateById(user);
    }

    @Test
    @DisplayName("resetPassword 用户不存在抛 404")
    void resetPasswordUserNotFound() {
        when(userMapper.selectById(404L)).thenReturn(null);
        BusinessException ex = assertThrows(BusinessException.class,
                () -> adminService.resetPassword(404L, "x"));
        assertEquals(404, ex.getCode());
        verify(userMapper, never()).updateById(any(User.class));
    }

    @Test
    @DisplayName("disableUser 将 enabled 置 0 并更新")
    void disableUser() {
        User user = new User();
        user.setId(3L);
        user.setEnabled(1);
        when(userMapper.selectById(3L)).thenReturn(user);

        adminService.disableUser(3L);

        assertEquals(0, user.getEnabled());
        verify(userMapper).updateById(user);
    }

    @Test
    @DisplayName("handleReport 处理后状态变更并记录处理人")
    void handleReport() {
        Report report = new Report();
        report.setId(11L);
        report.setStatus("PENDING");
        when(reportMapper.selectById(11L)).thenReturn(report);

        Report result = adminService.handleReport(11L, true, "属实", 100L);

        assertEquals("HANDLED", result.getStatus());
        assertEquals("属实", result.getHandleRemark());
        assertEquals(100L, result.getHandlerId());
        verify(reportMapper).updateById(report);
    }

    @Test
    @DisplayName("handleReport 驳回时状态为 REJECTED")
    void rejectReport() {
        Report report = new Report();
        report.setId(12L);
        when(reportMapper.selectById(12L)).thenReturn(report);
        Report result = adminService.handleReport(12L, false, "证据不足", 100L);
        assertEquals("REJECTED", result.getStatus());
    }

    @Test
    @DisplayName("handleReport 举报不存在抛 404")
    void handleReportNotFound() {
        when(reportMapper.selectById(999L)).thenReturn(null);
        assertThrows(BusinessException.class,
                () -> adminService.handleReport(999L, true, "", 1L));
    }

    @Test
    @DisplayName("dashboard 汇总核心指标并含 7 天注册趋势")
    void dashboard() {
        when(userMapper.selectCount(any())).thenReturn(50L);
        when(teamMapper.selectCount(any())).thenReturn(20L);
        when(competitionMapper.selectCount(any())).thenReturn(8L);
        when(taskMapper.selectCount(any())).thenReturn(120L);
        when(applyMapper.selectCount(any())).thenReturn(60L);
        when(reportMapper.selectCount(any())).thenReturn(2L);

        Map<String, Object> data = adminService.dashboard();

        assertEquals(50L, data.get("userCount"));
        assertEquals(20L, data.get("teamCount"));
        assertEquals(8L, data.get("competitionCount"));
        assertEquals(2L, data.get("reportPending"));
        assertTrue(data.containsKey("registerTrend"));
        assertEquals(7, ((List<?>) data.get("registerTrend")).size());
    }

    @Test
    @DisplayName("cleanLogs 统计并删除阈值前日志，返回删除条数")
    void cleanLogs() {
        when(operationLogMapper.selectCount(any())).thenReturn(15L);

        long deleted = adminService.cleanLogs(30);

        assertEquals(15L, deleted);
        verify(operationLogMapper).delete(any());
    }

    @Test
    @DisplayName("pageLogs 按状态过滤并返回分页结构")
    void pageLogs() {
        com.baomidou.mybatisplus.extension.plugins.pagination.Page<OperationLog> page =
                new com.baomidou.mybatisplus.extension.plugins.pagination.Page<>(1, 10);
        page.setRecords(List.of(new OperationLog()));
        page.setTotal(1);
        when(operationLogMapper.selectPage(any(), any())).thenReturn(page);

        Map<String, Object> result = adminService.pageLogs("alice", "POST", "FAIL", 1, 10);

        assertEquals(1L, result.get("total"));
        assertEquals(1, ((List<?>) result.get("records")).size());
    }

    @Test
    @DisplayName("saveSkill 新建走 insert，更新走 updateById")
    void saveSkill() {
        com.campuslink.entity.Skill newSkill = new com.campuslink.entity.Skill();
        adminService.saveSkill(newSkill);
        verify(skillMapper).insert(newSkill);

        com.campuslink.entity.Skill exist = new com.campuslink.entity.Skill();
        exist.setId(5L);
        adminService.saveSkill(exist);
        verify(skillMapper).updateById(exist);
        verify(skillMapper, times(1)).insert(any(com.campuslink.entity.Skill.class));
    }

    @Test
    @DisplayName("publishSystemNotice 填充发布人与发布时间")
    void publishSystemNotice() {
        SystemNotice notice = adminService.publishSystemNotice("标题", "正文", 1L);
        assertEquals("标题", notice.getTitle());
        assertEquals(1L, notice.getPublisherId());
        verify(systemNoticeMapper).insert(any(SystemNotice.class));
    }
}
