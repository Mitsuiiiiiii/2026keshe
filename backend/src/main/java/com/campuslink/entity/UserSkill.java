package com.campuslink.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 用户技能关联实体。
 */
@Data
@TableName("user_skill")
public class UserSkill {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private Long skillId;
}
