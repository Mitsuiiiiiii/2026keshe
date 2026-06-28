package com.campuslink.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 赛事日程时间表实体，v3 新增。
 *
 * <p>stage 取 SIGN_UP / PRELIM / SEMI / FINAL / AWARD 等阶段标识，由前端约定。
 *
 * @author liuguangyuan
 * @since 2026/6/29
 */
@Data
@TableName("competition_schedule")
public class CompetitionSchedule {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long competitionId;

    /** 阶段标识：SIGN_UP / PRELIM / SEMI / FINAL / AWARD 等 */
    private String stage;

    private String title;

    private LocalDateTime startTime;

    private LocalDateTime endTime;

    private String location;

    private String remark;

    private Integer sortOrder;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
