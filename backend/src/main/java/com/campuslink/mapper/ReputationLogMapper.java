package com.campuslink.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.campuslink.entity.ReputationLog;
import org.apache.ibatis.annotations.Mapper;

/**
 * 信誉分变动记录 Mapper，v3 新增。
 *
 * @author liuguangyuan
 * @since 2026/6/28
 */
@Mapper
public interface ReputationLogMapper extends BaseMapper<ReputationLog> {
}
