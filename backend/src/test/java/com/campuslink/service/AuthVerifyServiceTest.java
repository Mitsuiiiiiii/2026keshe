package com.campuslink.service;

import com.campuslink.common.BusinessException;
import com.campuslink.common.ResultCode;
import com.campuslink.entity.AuthVerifyCode;
import com.campuslink.entity.User;
import com.campuslink.mapper.AuthVerifyCodeMapper;
import com.campuslink.mapper.UserMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * {@link AuthVerifyService} 邮箱验证码服务单元测试（Mockito）。
 *
 * @author liuguangyuan
 * @since 2026/6/28
 */
class AuthVerifyServiceTest {

    private AuthVerifyCodeMapper codeMapper;
    private UserMapper userMapper;
    private MailService mailService;
    private PasswordEncoder passwordEncoder;
    private AuthVerifyService service;

    @BeforeEach
    void setUp() {
        codeMapper = mock(AuthVerifyCodeMapper.class);
        userMapper = mock(UserMapper.class);
        mailService = mock(MailService.class);
        passwordEncoder = mock(PasswordEncoder.class);
        service = new AuthVerifyService(codeMapper, userMapper, mailService, passwordEncoder);
    }

    @Test
    @DisplayName("sendCode 邮箱已注册时生成验证码并触发发送")
    void sendCodeSuccess() {
        when(userMapper.selectOne(any())).thenReturn(new User());
        service.sendCode("a@b.com", "RESET_PWD");
        verify(codeMapper).insert(any(AuthVerifyCode.class));
        verify(mailService).sendVerifyCode(eq("a@b.com"), any(), eq("RESET_PWD"));
    }

    @Test
    @DisplayName("sendCode 邮箱未注册时抛 404 且不发码")
    void sendCodeEmailNotRegistered() {
        when(userMapper.selectOne(any())).thenReturn(null);
        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.sendCode("none@b.com", "RESET_PWD"));
        assertEquals(404, ex.getCode());
        verify(codeMapper, never()).insert(any(AuthVerifyCode.class));
        verify(mailService, never()).sendVerifyCode(any(), any(), any());
    }

    @Test
    @DisplayName("verify 无匹配记录抛验证码无效")
    void verifyNoRecord() {
        when(codeMapper.selectOne(any())).thenReturn(null);
        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.verify("a@b.com", "000000", "RESET_PWD"));
        assertEquals(ResultCode.VERIFY_CODE_INVALID.getCode(), ex.getCode());
    }

    @Test
    @DisplayName("verify 过期记录抛已过期")
    void verifyExpired() {
        AuthVerifyCode record = new AuthVerifyCode();
        record.setId(1L);
        record.setExpireAt(LocalDateTime.now().minusMinutes(1));
        when(codeMapper.selectOne(any())).thenReturn(record);
        assertThrows(BusinessException.class,
                () -> service.verify("a@b.com", "123456", "RESET_PWD"));
        verify(codeMapper, never()).updateById(any(AuthVerifyCode.class));
    }

    @Test
    @DisplayName("verify 有效记录通过并标记已用")
    void verifyValid() {
        AuthVerifyCode record = new AuthVerifyCode();
        record.setId(1L);
        record.setExpireAt(LocalDateTime.now().plusMinutes(3));
        when(codeMapper.selectOne(any())).thenReturn(record);

        service.verify("a@b.com", "123456", "RESET_PWD");

        assertEquals(1, record.getUsed());
        verify(codeMapper).updateById(record);
    }

    @Test
    @DisplayName("resetPassword 校验通过后写入加密新密码")
    void resetPassword() {
        AuthVerifyCode record = new AuthVerifyCode();
        record.setId(1L);
        record.setExpireAt(LocalDateTime.now().plusMinutes(3));
        when(codeMapper.selectOne(any())).thenReturn(record);
        User user = new User();
        user.setId(9L);
        when(userMapper.selectOne(any())).thenReturn(user);
        when(passwordEncoder.encode("newPass")).thenReturn("ENC");

        service.resetPassword("a@b.com", "123456", "newPass");

        assertEquals("ENC", user.getPassword());
        verify(userMapper).updateById(user);
    }
}
