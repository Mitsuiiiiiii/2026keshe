package com.campuslink.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 队伍关键事件（动态墙时间线）实体，v3 新增。
 *
 * @author liuguangyuan
 * @since 2026/6/29
 */
@Data
@TableName("team_event")
public class TeamEvent {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long teamId;

    /** 事件类型，如 POST / AWARD / JOIN 等 */
    private String type;

    private String content;

    /** 触发人 */
    private Long actorId;

    /** 关联业务 id */
    private Long refId;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
