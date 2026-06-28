package com.campuslink.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.campuslink.entity.CompetitionSchedule;
import org.apache.ibatis.annotations.Mapper;

/**
 * 赛事日程 Mapper，v3 新增。
 *
 * @author liuguangyuan
 * @since 2026/6/29
 */
@Mapper
public interface CompetitionScheduleMapper extends BaseMapper<CompetitionSchedule> {
}
