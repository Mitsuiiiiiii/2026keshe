package com.campuslink.common;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * {@link BusinessException} 与 {@link ResultCode} 单元测试。
 *
 * @author liuguangyuan
 * @since 2026/6/28
 */
class BusinessExceptionTest {

    @Test
    @DisplayName("仅传 message 时默认 400")
    void messageOnly() {
        BusinessException e = new BusinessException("出错了");
        assertEquals(400, e.getCode());
        assertEquals("出错了", e.getMessage());
    }

    @Test
    @DisplayName("传 ResultCode 时取其 code 与 message")
    void withResultCode() {
        BusinessException e = new BusinessException(ResultCode.ACCOUNT_LOCKED);
        assertEquals(4001, e.getCode());
        assertEquals("账号已锁定，请稍后再试", e.getMessage());
    }

    @Test
    @DisplayName("ResultCode + 自定义 message")
    void withResultCodeAndMessage() {
        BusinessException e = new BusinessException(ResultCode.TOO_MANY_REQUESTS, "登录太频繁");
        assertEquals(429, e.getCode());
        assertEquals("登录太频繁", e.getMessage());
    }

    @Test
    @DisplayName("新增限流状态码 429 已注册")
    void tooManyRequestsCode() {
        assertEquals(429, ResultCode.TOO_MANY_REQUESTS.getCode());
        assertEquals("操作过于频繁，请稍后再试", ResultCode.TOO_MANY_REQUESTS.getMessage());
    }
}
