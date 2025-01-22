package com.melissa.diary.security;

import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;

import java.util.Collection;

/**
 * JWT 인증 후, SecurityContextHolder에 저장할 Custom Authentication
 */
public class JwtUserAuthentication extends AbstractAuthenticationToken {

    private final Long userId;
    private final Object credentials;  // 비밀번호 혹은 null
    private boolean authenticated;

    // 생성자
    public JwtUserAuthentication(Long userId, Object credentials, Collection<? extends GrantedAuthority> authorities) {
        super(authorities);
        this.userId = userId;
        this.credentials = credentials;
        // 인증을 직접 세팅할 경우 true
        super.setAuthenticated(true);
    }

    @Override
    public Object getCredentials() {
        return this.credentials;
    }

    @Override
    public Object getPrincipal() {
        // 인증 주체를 userId로 설정
        return this.userId;
    }

    @Override
    public boolean isAuthenticated() {
        return authenticated;
    }

    @Override
    public void setAuthenticated(boolean isAuthenticated) {
        this.authenticated = isAuthenticated;
        super.setAuthenticated(isAuthenticated);
    }
}