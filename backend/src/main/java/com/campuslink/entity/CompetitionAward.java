package com.campuslink.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 赛事获奖公示实体，v3 新增。
 *
 * <p>awardLevel 取 FIRST / SECOND / THIRD / EXCELLENT 等奖项等级，由前端约定。
 * members 存获奖成员姓名（逗号分隔或 JSON 文本）。
 *
 * @author liuguangyuan
 * @since 2026/6/29
 */
@Data
@TableName("competition_award")
public class CompetitionAward {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long competitionId;

    private Long teamId;

    private String teamName;

    /** 奖项等级：FIRST / SECOND / THIRD / EXCELLENT 等 */
    private String awardLevel;

    private Integer awardYear;

    private Integer memberCount;

    private String members;

    private Long publisherId;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
