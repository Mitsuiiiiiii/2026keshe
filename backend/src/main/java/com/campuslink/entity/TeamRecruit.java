package com.campuslink.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 招募岗位实体。
 */
@Data
@TableName("team_recruit")
public class TeamRecruit {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long teamId;

    private String position;

    private Integer count;

    private Integer filled;

    /** 招募标签，逗号分隔，供前端高亮 */
    private String tags;

    /** 是否置顶：0否 1是 */
    private Integer isTop;
}
