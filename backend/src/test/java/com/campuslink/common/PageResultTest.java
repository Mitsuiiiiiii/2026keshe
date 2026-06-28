package com.campuslink.common;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * {@link PageResult} 分页返回体单元测试。
 *
 * @author liuguangyuan
 * @since 2026/6/28
 */
class PageResultTest {

    @Test
    @DisplayName("of() 组装分页四要素")
    void of() {
        PageResult<String> result = PageResult.of(List.of("a", "b"), 10L, 1L, 5L);
        assertEquals(2, ((List<?>) result.get("records")).size());
        assertEquals(10L, result.get("total"));
        assertEquals(1L, result.get("current"));
        assertEquals(5L, result.get("size"));
    }

    @Test
    @DisplayName("empty() 返回空记录且 total=0")
    void empty() {
        PageResult<String> result = PageResult.empty(2L, 20L);
        assertTrue(((List<?>) result.get("records")).isEmpty());
        assertEquals(0L, result.get("total"));
        assertEquals(2L, result.get("current"));
        assertEquals(20L, result.get("size"));
    }

    @Test
    @DisplayName("wrap() 包装 MyBatis-Plus IPage")
    void wrap() {
        Page<String> page = new Page<>(3, 10);
        page.setRecords(List.of("x"));
        page.setTotal(31);
        Map<String, Object> map = PageResult.wrap(page);
        assertEquals(1, ((List<?>) map.get("records")).size());
        assertEquals(31L, map.get("total"));
        assertEquals(3L, map.get("current"));
        assertEquals(10L, map.get("size"));
    }
}
