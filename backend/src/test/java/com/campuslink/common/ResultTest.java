package com.campuslink.common;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * {@link Result} 统一返回体单元测试。
 *
 * @author liuguangyuan
 * @since 2026/6/28
 */
class ResultTest {

    @Test
    @DisplayName("success() 无数据时 code=200、data=null")
    void successWithoutData() {
        Result<Void> result = Result.success();
        assertEquals(200, result.getCode());
        assertEquals("success", result.getMessage());
        assertNull(result.getData());
    }

    @Test
    @DisplayName("success(data) 携带数据")
    void successWithData() {
        Result<String> result = Result.success("hello");
        assertEquals(200, result.getCode());
        assertEquals("hello", result.getData());
    }

    @Test
    @DisplayName("success(message, data) 自定义提示")
    void successWithMessageAndData() {
        Result<Integer> result = Result.success("ok", 42);
        assertEquals(200, result.getCode());
        assertEquals("ok", result.getMessage());
        assertEquals(42, result.getData());
    }

    @Test
    @DisplayName("error(ResultCode) 映射状态码与默认文案")
    void errorWithResultCode() {
        Result<Void> result = Result.error(ResultCode.FORBIDDEN);
        assertEquals(403, result.getCode());
        assertEquals("无权限访问", result.getMessage());
        assertNull(result.getData());
    }

    @Test
    @DisplayName("error(ResultCode, message) 覆盖文案")
    void errorWithCustomMessage() {
        Result<Void> result = Result.error(ResultCode.BAD_REQUEST, "字段缺失");
        assertEquals(400, result.getCode());
        assertEquals("字段缺失", result.getMessage());
    }

    @Test
    @DisplayName("error(code, message) 纯数字状态码")
    void errorWithRawCode() {
        Result<Void> result = Result.error(429, "太频繁");
        assertEquals(429, result.getCode());
        assertEquals("太频繁", result.getMessage());
    }
}
