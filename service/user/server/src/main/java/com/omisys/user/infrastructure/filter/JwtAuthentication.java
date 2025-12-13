package com.omisys.user.infrastructure.filter;

import com.omisys.auth.server.auth_dto.jwt.JwtClaim;
import lombok.Builder;
import org.jspecify.annotations.Nullable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.Collection;
import java.util.Collections;

@Builder
public class JwtAuthentication implements Authentication {

    private Long userId;
    private String username;
    private String role;

    public static JwtAuthentication create(JwtClaim claims) {
        return JwtAuthentication.builder()
                .userId(claims.getUserId())
                .username(claims.getUsername())
                .role(claims.getRole())
                .build();
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return Collections.singleton(new SimpleGrantedAuthority(role));
    }

    @Override
    public @Nullable Object getCredentials() {
        return null;
    }

    @Override
    public @Nullable Object getDetails() {
        return null;
    }

    @Override
    public @Nullable Object getPrincipal() {
        return null;
    }

    @Override
    public boolean isAuthenticated() {
        return false;
    }

    @Override
    public void setAuthenticated(boolean isAuthenticated) throws IllegalArgumentException {

    }

    @Override
    public String getName() {
        return "";
    }
}
