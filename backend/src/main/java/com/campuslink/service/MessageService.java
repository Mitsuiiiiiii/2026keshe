package com.campuslink.service;

import com.campuslink.entity.Message;
import com.campuslink.mapper.MessageMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * 站内消息服务。Day6-8 提供消息写入能力（申请 / 审核 / 公告 / 任务推送），
 * 完整消息中心（列表 / 未读计数 / 已读）在 Day9-10 实现。
 */
@Service
@RequiredArgsConstructor
public class MessageService {

    private final MessageMapper messageMapper;

    /**
     * 创建一条站内消息。
     *
     * @param receiverId 接收人
     * @param type       APPLY / AUDIT / NOTICE / TASK
     * @param content    消息内容
     * @param refId      关联业务 id
     */
    public void create(Long receiverId, String type, String content, Long refId) {
        Message message = new Message();
        message.setReceiverId(receiverId);
        message.setType(type);
        message.setContent(content);
        message.setRefId(refId);
        message.setIsRead(0);
        messageMapper.insert(message);
    }
}
