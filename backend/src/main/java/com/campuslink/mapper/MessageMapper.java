package com.campuslink.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.campuslink.entity.Message;
import org.apache.ibatis.annotations.Mapper;

/**
 * 站内消息 Mapper。
 */
@Mapper
public interface MessageMapper extends BaseMapper<Message> {
}
