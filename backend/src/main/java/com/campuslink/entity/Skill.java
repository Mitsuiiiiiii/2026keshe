package com.campuslink.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 技能库实体。
 */
@Data
@TableName("skill")
public class Skill {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String name;

    private String category;
}
