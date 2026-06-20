package com.campuslink.security;

import lombok.AllArgsConstructor;
import lombok.Data;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

/**
 * 登录用户主体，存入 SecurityContext。
 */
@Data
@AllArgsConstructor
public class LoginUser implements UserDetails {

    private Long userId;
    private String username;
    private String role;

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        // Spring Security 角色需以 ROLE_ 前缀，便于 hasRole('ADMIN') 判断
        return List.of(new SimpleGrantedAuthority("ROLE_" + role));
    }

    @Override
    public String getPassword() {
        return null;
    }

    @Override
    public String getUsername() {
        return username;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }
}
