package com.campuslink.common;

import lombok.Data;

/**
 * 分页查询基础参数。
 */
@Data
public class PageQuery {

    /** 当前页，从 1 开始 */
    private Integer current = 1;

    /** 每页条数 */
    private Integer size = 10;

    /** 关键词（可选） */
    private String keyword;
}
