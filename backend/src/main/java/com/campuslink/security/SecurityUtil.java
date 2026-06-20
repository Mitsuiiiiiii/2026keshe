package com.campuslink.security;

import com.campuslink.common.BusinessException;
import com.campuslink.common.ResultCode;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * 获取当前登录用户的工具。
 */
public class SecurityUtil {

    public static LoginUser getLoginUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof LoginUser loginUser)) {
            throw new BusinessException(ResultCode.UNAUTHORIZED);
        }
        return loginUser;
    }

    public static Long getUserId() {
        return getLoginUser().getUserId();
    }

    public static boolean isAdmin() {
        return "ADMIN".equals(getLoginUser().getRole());
    }
}
