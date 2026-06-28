package com.campuslink.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 信誉分变动记录实体，v3 新增。
 *
 * <p>每次用户信誉分发生变动时写入一条，用于“变动记录”展示。
 *
 * @author liuguangyuan
 * @since 2026/6/28
 */
@Data
@TableName("reputation_log")
public class ReputationLog {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    /** 本次变动值（after - before）。 */
    private BigDecimal changeValue;

    private BigDecimal beforeValue;

    private BigDecimal afterValue;

    /** 变动来源类型，如 EVAL。 */
    private String sourceType;

    private String reason;

    /** 关联业务 id，如来源评价的 evaluation.id。 */
    private Long refId;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
