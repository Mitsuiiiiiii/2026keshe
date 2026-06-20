package com.campuslink.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.campuslink.entity.User;
import org.apache.ibatis.annotations.Mapper;

/**
 * 用户 Mapper。
 */
@Mapper
public interface UserMapper extends BaseMapper<User> {
}
