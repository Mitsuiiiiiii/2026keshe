package com.campuslink.security;

import com.campuslink.common.Result;
import com.campuslink.common.ResultCode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * 鉴权异常的 JSON 响应：401 未登录、403 无权限。
 */
public class RestAuthHandlers {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private static void write(HttpServletResponse response, ResultCode code) throws IOException {
        response.setStatus(HttpServletResponse.SC_OK);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.getWriter().write(MAPPER.writeValueAsString(Result.error(code)));
    }

    /** 未登录 401 */
    @Component
    public static class RestAuthEntryPoint implements AuthenticationEntryPoint {
        @Override
        public void commence(HttpServletRequest request, HttpServletResponse response,
                             AuthenticationException authException) throws IOException {
            write(response, ResultCode.UNAUTHORIZED);
        }
    }

    /** 无权限 403 */
    @Component
    public static class RestAccessDeniedHandler implements AccessDeniedHandler {
        @Override
        public void handle(HttpServletRequest request, HttpServletResponse response,
                           AccessDeniedException accessDeniedException) throws IOException {
            write(response, ResultCode.FORBIDDEN);
        }
    }
}
