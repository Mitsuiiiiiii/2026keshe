package com.campuslink.security;

import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * {@link JwtUtil} 单元测试。通过反射注入 {@code secret} / {@code expiration}，
 * 不依赖 Spring 容器。
 *
 * @author liuguangyuan
 * @since 2026/6/28
 */
class JwtUtilTest {

    private JwtUtil jwtUtil;

    @BeforeEach
    void setUp() {
        jwtUtil = new JwtUtil();
        ReflectionTestUtils.setField(jwtUtil, "secret",
                "campuslink-secret-key-for-jwt-signing-must-be-long-enough-2026");
        ReflectionTestUtils.setField(jwtUtil, "expiration", 3_600_000L);
    }

    @Test
    @DisplayName("生成的 token 可被解析且 claims 一致")
    void generateAndParse() {
        String token = jwtUtil.generateToken(99L, "alice", "ADMIN");
        Claims claims = jwtUtil.parse(token);
        assertEquals("99", claims.getSubject());
        assertEquals("alice", claims.get("username"));
        assertEquals("ADMIN", claims.get("role"));
    }

    @Test
    @DisplayName("合法 token 校验通过")
    void validateValidToken() {
        String token = jwtUtil.generateToken(1L, "bob", "USER");
        assertTrue(jwtUtil.validate(token));
    }

    @Test
    @DisplayName("被篡改 token 校验失败")
    void validateTamperedToken() {
        String token = jwtUtil.generateToken(1L, "bob", "USER");
        assertFalse(jwtUtil.validate(token + "tampered"));
    }

    @Test
    @DisplayName("已过期 token 校验失败")
    void validateExpiredToken() {
        ReflectionTestUtils.setField(jwtUtil, "expiration", -1_000L);
        String expired = jwtUtil.generateToken(1L, "bob", "USER");
        assertFalse(jwtUtil.validate(expired));
    }
}
