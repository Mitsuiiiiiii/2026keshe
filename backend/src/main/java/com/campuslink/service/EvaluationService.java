package com.campuslink.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.campuslink.common.BusinessException;
import com.campuslink.common.ResultCode;
import com.campuslink.entity.Evaluation;
import com.campuslink.entity.EvaluationReply;
import com.campuslink.entity.ReputationLog;
import com.campuslink.entity.Report;
import com.campuslink.entity.User;
import com.campuslink.mapper.EvaluationMapper;
import com.campuslink.mapper.EvaluationReplyMapper;
import com.campuslink.mapper.ReportMapper;
import com.campuslink.mapper.ReputationLogMapper;
import com.campuslink.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 评价业务，v2 新增。
 *
 * <p>规则：
 * 1. 提交评价：写 evaluation + 更新被评价人 reputation（平均分换算 5 分制）
 * 2. 回复评价 / 举报评价
 * 3. 信誉分明细 / 排行榜
 *
 * @author liuguangyuan
 * @since 2026/6/27
 */
@Service
@RequiredArgsConstructor
public class EvaluationService {

    private static final Logger LOGGER = LoggerFactory.getLogger(EvaluationService.class);

    private final EvaluationMapper evaluationMapper;
    private final EvaluationReplyMapper replyMapper;
    private final ReportMapper reportMapper;
    private final UserMapper userMapper;
    private final ReputationLogMapper reputationLogMapper;

    public Evaluation submit(Long teamId, Long fromUserId, Long toUserId,
                             int responsibility, int tech, int communication, Integer anonymous) {
        if (fromUserId.equals(toUserId)) {
            throw new BusinessException("不能给自己评价");
        }

        Long exists = evaluationMapper.selectCount(new LambdaQueryWrapper<Evaluation>()
                .eq(Evaluation::getTeamId, teamId)
                .eq(Evaluation::getFromUserId, fromUserId)
                .eq(Evaluation::getToUserId, toUserId));
        if (exists != null && exists > 0) {
            throw new BusinessException(ResultCode.DUPLICATE_RECORD, "已评价过该成员");
        }

        Evaluation eval = new Evaluation();
        eval.setTeamId(teamId);
        eval.setFromUserId(fromUserId);
        eval.setToUserId(toUserId);
        eval.setResponsibility(responsibility);
        eval.setTech(tech);
        eval.setCommunication(communication);
        eval.setAnonymous(anonymous == null ? 0 : (anonymous != 0 ? 1 : 0));
        evaluationMapper.insert(eval);
        LOGGER.info("评价已提交, evalId={}, toUserId={}, anonymous={}", eval.getId(), toUserId, eval.getAnonymous());

        recalcReputation(toUserId, eval.getId());
        return eval;
    }

    /**
     * 切换某条评价的匿名状态（仅评价人本人可切换自己的评价）。
     */
    public Evaluation toggleAnonymous(Long evaluationId, Integer anonymous, Long currentUserId) {
        Evaluation eval = evaluationMapper.selectById(evaluationId);
        if (eval == null) {
            throw new BusinessException(ResultCode.NOT_FOUND);
        }
        if (!eval.getFromUserId().equals(currentUserId)) {
            throw new BusinessException(ResultCode.FORBIDDEN, "只能切换自己的评价");
        }
        eval.setAnonymous(anonymous != null && anonymous != 0 ? 1 : 0);
        evaluationMapper.updateById(eval);
        LOGGER.info("评价匿名状态已切换, evalId={}, anonymous={}", evaluationId, eval.getAnonymous());
        return eval;
    }

    public List<Evaluation> listByUser(Long userId) {
        List<Evaluation> list = evaluationMapper.selectList(new LambdaQueryWrapper<Evaluation>()
                .eq(Evaluation::getToUserId, userId)
                .orderByDesc(Evaluation::getCreateTime));
        return maskAnonymous(list);
    }

    /**
     * 我发出的评价（不脱敏，便于本人查看并切换匿名）。回填被评价人昵称。
     */
    public List<Evaluation> listSent(Long fromUserId) {
        List<Evaluation> list = evaluationMapper.selectList(new LambdaQueryWrapper<Evaluation>()
                .eq(Evaluation::getFromUserId, fromUserId)
                .orderByDesc(Evaluation::getCreateTime));
        for (Evaluation e : list) {
            User to = userMapper.selectById(e.getToUserId());
            e.setToUserName(to == null ? null : to.getNickname());
        }
        return list;
    }

    /**
     * 处理匿名展示：anonymous=1 时隐藏评价人真实身份（fromUserId 置空、fromUserName 显示“匿名用户”），
     * 实名时回填评价人昵称。toUser 不受影响。
     */
    private List<Evaluation> maskAnonymous(List<Evaluation> list) {
        for (Evaluation e : list) {
            if (e.getAnonymous() != null && e.getAnonymous() == 1) {
                e.setFromUserId(null);
                e.setFromUserName("匿名用户");
            } else if (e.getFromUserId() != null) {
                User from = userMapper.selectById(e.getFromUserId());
                e.setFromUserName(from == null ? null : from.getNickname());
            }
        }
        return list;
    }

    /**
     * 查询某用户信誉分变动记录（时间倒序）。
     */
    public List<ReputationLog> reputationLog(Long userId) {
        return reputationLogMapper.selectList(new LambdaQueryWrapper<ReputationLog>()
                .eq(ReputationLog::getUserId, userId)
                .orderByDesc(ReputationLog::getCreateTime));
    }

    public EvaluationReply reply(Long evaluationId, String content, Long authorId) {
        Evaluation eval = evaluationMapper.selectById(evaluationId);
        if (eval == null) {
            throw new BusinessException(ResultCode.NOT_FOUND);
        }
        EvaluationReply reply = new EvaluationReply();
        reply.setEvaluationId(evaluationId);
        reply.setAuthorId(authorId);
        reply.setContent(content);
        replyMapper.insert(reply);
        LOGGER.info("评价回复已提交, evalId={}, authorId={}", evaluationId, authorId);
        return reply;
    }

    public List<EvaluationReply> listReplies(Long evaluationId) {
        return replyMapper.selectList(new LambdaQueryWrapper<EvaluationReply>()
                .eq(EvaluationReply::getEvaluationId, evaluationId)
                .orderByAsc(EvaluationReply::getCreateTime));
    }

    public Report report(Long evaluationId, String reason, Long reporterId) {
        if (evaluationMapper.selectById(evaluationId) == null) {
            throw new BusinessException(ResultCode.NOT_FOUND);
        }
        Report report = new Report();
        report.setTargetType("EVALUATION");
        report.setTargetId(evaluationId);
        report.setReporterId(reporterId);
        report.setReason(reason);
        report.setStatus("PENDING");
        reportMapper.insert(report);
        LOGGER.info("评价已举报, reportId={}, evalId={}", report.getId(), evaluationId);
        return report;
    }

    /** 信誉分明细 */
    public Map<String, Object> reputationDetail(Long userId) {
        List<Evaluation> list = listByUser(userId);
        if (list.isEmpty()) {
            Map<String, Object> empty = new HashMap<>();
            empty.put("total", 0);
            empty.put("avgResponsibility", 0);
            empty.put("avgTech", 0);
            empty.put("avgCommunication", 0);
            empty.put("items", List.of());
            return empty;
        }

        double respSum = list.stream().mapToInt(Evaluation::getResponsibility).sum();
        double techSum = list.stream().mapToInt(Evaluation::getTech).sum();
        double commSum = list.stream().mapToInt(Evaluation::getCommunication).sum();
        double avgResp = respSum / list.size();
        double avgTech = techSum / list.size();
        double avgComm = commSum / list.size();
        Map<String, Object> data = new HashMap<>();
        data.put("total", list.size());
        data.put("avgResponsibility", round(avgResp));
        data.put("avgTech", round(avgTech));
        data.put("avgCommunication", round(avgComm));
        data.put("items", list);

        // 加权计算明细：当前规则三维等权（各 1/3），透明展示每维加权贡献与最终信誉分
        double w = 1.0 / 3.0;
        List<Map<String, Object>> weightDetail = new ArrayList<>();
        weightDetail.add(weightRow("责任心", round(avgResp), w, round(avgResp * w)));
        weightDetail.add(weightRow("技术能力", round(avgTech), w, round(avgTech * w)));
        weightDetail.add(weightRow("沟通能力", round(avgComm), w, round(avgComm * w)));
        data.put("weightDetail", weightDetail);
        data.put("weightedScore", round(avgResp * w + avgTech * w + avgComm * w));
        data.put("formula", "信誉分 = 责任心均分×1/3 + 技术能力均分×1/3 + 沟通能力均分×1/3");
        return data;
    }

    private Map<String, Object> weightRow(String dim, BigDecimal avg, double weight, BigDecimal contribution) {
        Map<String, Object> row = new HashMap<>();
        row.put("dimension", dim);
        row.put("avg", avg);
        row.put("weight", round(weight));
        row.put("contribution", contribution);
        return row;
    }

    /** 信誉分排行榜 */
    public List<Map<String, Object>> ranking(int limit) {
        List<User> users = userMapper.selectList(new LambdaQueryWrapper<User>()
                .eq(User::getRole, "STUDENT")
                .orderByDesc(User::getReputation));
        List<Map<String, Object>> result = new ArrayList<>();
        int top = Math.min(limit <= 0 ? 50 : limit, users.size());
        for (int i = 0; i < top; i++) {
            User u = users.get(i);
            Map<String, Object> item = new HashMap<>();
            item.put("rank", i + 1);
            item.put("userId", u.getId());
            item.put("nickname", u.getNickname());
            item.put("reputation", u.getReputation());
            item.put("college", u.getCollege());
            result.add(item);
        }
        result.sort(Comparator.comparingInt(o -> ((Number) o.get("rank")).intValue()));
        return result;
    }

    private void recalcReputation(Long userId, Long refEvalId) {
        List<Evaluation> list = listByUser(userId);
        if (list.isEmpty()) {
            return;
        }
        double avg = list.stream()
                .mapToDouble(e -> (e.getResponsibility() + e.getTech() + e.getCommunication()) / 3.0)
                .average()
                .orElse(5.0);
        User user = userMapper.selectById(userId);
        if (user != null) {
            BigDecimal before = user.getReputation() == null ? BigDecimal.ZERO : user.getReputation();
            BigDecimal after = round(avg);
            user.setReputation(after);
            userMapper.updateById(user);
            LOGGER.info("信誉分已重算, userId={}, reputation={}", userId, after);

            if (before.compareTo(after) != 0) {
                ReputationLog log = new ReputationLog();
                log.setUserId(userId);
                log.setBeforeValue(before);
                log.setAfterValue(after);
                log.setChangeValue(after.subtract(before));
                log.setSourceType("EVAL");
                log.setReason("收到新评价，信誉分按各维度平均分重算");
                log.setRefId(refEvalId);
                reputationLogMapper.insert(log);
                LOGGER.info("信誉分变动记录已写入, userId={}, before={}, after={}", userId, before, after);
            }
        }
    }

    private BigDecimal round(double v) {
        return BigDecimal.valueOf(v).setScale(2, RoundingMode.HALF_UP);
    }
}
