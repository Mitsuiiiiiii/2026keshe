package com.campuslink.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.campuslink.entity.TeamEvent;
import org.apache.ibatis.annotations.Mapper;

/**
 * 队伍事件时间线 Mapper，v3 新增。
 *
 * @author liuguangyuan
 * @since 2026/6/29
 */
@Mapper
public interface TeamEventMapper extends BaseMapper<TeamEvent> {
}
