package com.campuslink.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.campuslink.common.PageResult;
import com.campuslink.entity.Message;
import com.campuslink.entity.Notice;
import com.campuslink.entity.UserFavorite;
import com.campuslink.entity.UserPrivacy;
import com.campuslink.mapper.MessageMapper;
import com.campuslink.mapper.NoticeMapper;
import com.campuslink.mapper.UserFavoriteMapper;
import com.campuslink.mapper.UserPrivacyMapper;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 消息中心拓展业务（分类筛选 / 批量已读 / 批量删除 / 关键词检索 / 定时公告），v2 新增。
 *
 * @author liuguangyuan
 * @since 2026/6/27
 */
@Service
@RequiredArgsConstructor
public class MessageExtraService {

    private static final Logger LOGGER = LoggerFactory.getLogger(MessageExtraService.class);

    private final MessageMapper messageMapper;
    private final NoticeMapper noticeMapper;
    private final UserFavoriteMapper userFavoriteMapper;
    private final UserPrivacyMapper userPrivacyMapper;

    public Map<String, Object> listMessages(Long userId, String type, Integer isRead, long current, long size) {
        LambdaQueryWrapper<Message> wrapper = new LambdaQueryWrapper<Message>()
                .eq(Message::getReceiverId, userId)
                .orderByDesc(Message::getCreateTime);
        if (type != null && !type.isBlank()) {
            wrapper.eq(Message::getType, type);
        }
        if (isRead != null) {
            wrapper.eq(Message::getIsRead, isRead);
        }
        Page<Message> page = messageMapper.selectPage(new Page<>(current, size), wrapper);
        return PageResult.wrap(page);
    }

    public long unreadCount(Long userId) {
        Long cnt = messageMapper.selectCount(new LambdaQueryWrapper<Message>()
                .eq(Message::getReceiverId, userId)
                .eq(Message::getIsRead, 0));
        return cnt == null ? 0 : cnt;
    }

    public int markRead(Long userId, Long id) {
        return messageMapper.update(null, new LambdaUpdateWrapper<Message>()
                .eq(Message::getId, id)
                .eq(Message::getReceiverId, userId)
                .set(Message::getIsRead, 1));
    }

    public int markAllRead(Long userId) {
        int n = messageMapper.update(null, new LambdaUpdateWrapper<Message>()
                .eq(Message::getReceiverId, userId)
                .eq(Message::getIsRead, 0)
                .set(Message::getIsRead, 1));
        LOGGER.info("全部已读, userId={}, 更新条数={}", userId, n);
        return n;
    }

    public int batchRead(Long userId, List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return 0;
        }
        return messageMapper.update(null, new LambdaUpdateWrapper<Message>()
                .eq(Message::getReceiverId, userId)
                .in(Message::getId, ids)
                .set(Message::getIsRead, 1));
    }

    public int batchDelete(Long userId, List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return 0;
        }
        return messageMapper.delete(new LambdaQueryWrapper<Message>()
                .eq(Message::getReceiverId, userId)
                .in(Message::getId, ids));
    }

    public Map<String, Object> search(Long userId, String keyword, long current, long size) {
        Page<Message> page = messageMapper.selectPage(new Page<>(current, size),
                new LambdaQueryWrapper<Message>()
                        .eq(Message::getReceiverId, userId)
                        .like(keyword != null && !keyword.isBlank(), Message::getContent, keyword)
                        .orderByDesc(Message::getCreateTime));
        return PageResult.wrap(page);
    }

    /** 定时公告 */
    public Notice scheduleNotice(Long teamId, Long authorId, String title, String content, LocalDateTime publishAt) {
        Notice notice = new Notice();
        notice.setTeamId(teamId);
        notice.setAuthorId(authorId);
        notice.setTitle(title);
        notice.setContent(content);
        notice.setPublishAt(publishAt);
        notice.setScheduled(1);
        noticeMapper.insert(notice);
        LOGGER.info("定时公告已创建, noticeId={}, publishAt={}", notice.getId(), publishAt);
        return notice;
    }

    /**
     * 收藏提醒 / 订阅推送对外契约方法，供其它模块在被收藏对象（竞赛 / 队伍等）发生更新时调用。
     *
     * <p>查 user_favorite 中 ref_type=targetType 且 ref_id=targetId 的所有收藏者，
     * 对其中 user_privacy.push_favorite=1 的用户插入一条 FAVORITE 类型站内消息。
     * 推送失败仅记日志，不影响主流程。
     *
     * @param targetType 收藏对象类型，对应 user_favorite.ref_type（如 COMPETITION / TEAM）
     * @param targetId   收藏对象 id，对应 user_favorite.ref_id
     * @param content    推送内容
     */
    public void notifyFavoriters(String targetType, Long targetId, String content) {
        try {
            if (targetType == null || targetId == null) {
                return;
            }
            List<UserFavorite> favorites = userFavoriteMapper.selectList(
                    new LambdaQueryWrapper<UserFavorite>()
                            .eq(UserFavorite::getRefType, targetType)
                            .eq(UserFavorite::getRefId, targetId));
            if (favorites.isEmpty()) {
                return;
            }
            List<Long> userIds = favorites.stream()
                    .map(UserFavorite::getUserId)
                    .distinct()
                    .collect(Collectors.toList());

            // 查询开启了收藏推送（push_favorite=1）的用户
            List<UserPrivacy> privacyList = userPrivacyMapper.selectList(
                    new LambdaQueryWrapper<UserPrivacy>()
                            .in(UserPrivacy::getUserId, userIds)
                            .eq(UserPrivacy::getPushFavorite, 1));
            for (UserPrivacy privacy : privacyList) {
                Message message = new Message();
                message.setReceiverId(privacy.getUserId());
                message.setType("FAVORITE");
                message.setContent(content);
                message.setRefId(targetId);
                message.setIsRead(0);
                messageMapper.insert(message);
            }
            LOGGER.info("收藏推送完成, targetType={}, targetId={}, 推送人数={}",
                    targetType, targetId, privacyList.size());
        } catch (Exception e) {
            LOGGER.warn("收藏推送失败, targetType={}, targetId={}, err={}",
                    targetType, targetId, e.getMessage());
        }
    }

    /**
     * 发送一条私信（一键私聊队长等场景）。
     *
     * <p>message 表无独立 sender 列，PRIVATE 类型用 ref_id 存发送者 id。
     */
    public Message sendPrivate(Long senderId, Long receiverId, String content) {
        Message message = new Message();
        message.setReceiverId(receiverId);
        message.setType("PRIVATE");
        message.setContent(content);
        message.setRefId(senderId);
        message.setIsRead(0);
        messageMapper.insert(message);
        LOGGER.info("私信已发送, senderId={}, receiverId={}, messageId={}",
                senderId, receiverId, message.getId());
        return message;
    }

    /**
     * 查询当前用户与某人的私信往来（双向，按时间正序）。
     *
     * <p>PRIVATE 类型中 receiver_id 为接收者，ref_id 为发送者。
     */
    public List<Message> listPrivate(Long userId, Long withUserId) {
        return messageMapper.selectList(new LambdaQueryWrapper<Message>()
                .eq(Message::getType, "PRIVATE")
                .and(w -> w
                        .or(q -> q.eq(Message::getReceiverId, userId).eq(Message::getRefId, withUserId))
                        .or(q -> q.eq(Message::getReceiverId, withUserId).eq(Message::getRefId, userId)))
                .orderByAsc(Message::getCreateTime));
    }
}
