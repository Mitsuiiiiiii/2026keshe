package com.campuslink.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 互评实体（v1 表已存在但缺少 Entity 映射，v2 补全）。
 *
 * @author liuguangyuan
 * @since 2026/6/27
 */
@Data
@TableName("evaluation")
public class Evaluation {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long teamId;

    private Long fromUserId;

    private Long toUserId;

    private Integer responsibility;

    private Integer tech;

    private Integer communication;

    /** 是否匿名（0实名/1匿名），对应 evaluation.anonymous 真实列（v3 增量脚本已追加）。 */
    private Integer anonymous;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /** 评价人展示名（非数据库列）：匿名时为“匿名用户”，实名时为评价人昵称。 */
    @TableField(exist = false)
    private String fromUserName;

    /** 被评价人展示名（非数据库列），用于“我发出的评价”列表。 */
    @TableField(exist = false)
    private String toUserName;
}
