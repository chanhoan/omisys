package com.omisys.auth.server.infrastructure.oauth;

import com.omisys.auth.server.application.dto.AuthResponse;
import com.omisys.auth.server.application.service.AuthService;
import com.omisys.auth.server.infrastructure.properties.JwtProperties;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class OAuth2AuthenticationSuccessHandler implements AuthenticationSuccessHandler {

    private static final String ACCESS_TOKEN_COOKIE = "accessToken";
    private static final String REFRESH_TOKEN_COOKIE = "refreshToken";

    private final AuthService authService;
    private final JwtProperties jwtProperties;

    @Override
    public void onAuthenticationSuccess(
            HttpServletRequest request,
            HttpServletResponse response,
            Authentication authentication) throws IOException, ServletException {
        OAuth2User principal = (OAuth2User) authentication.getPrincipal();
        String email = principal.getAttribute("email");
        AuthResponse.TokenPair tokenPair = authService.signInByOAuth2Email(email);
        setTokenCookies(response, tokenPair);
        response.setStatus(HttpServletResponse.SC_OK);
    }

    private void setTokenCookies(HttpServletResponse response, AuthResponse.TokenPair tokenPair) {
        boolean secure = jwtProperties.isCookieSecure();
        int accessMaxAge = (int) (jwtProperties.getAccessTokenExpiresIn() / 1000);
        int refreshMaxAge = (int) (jwtProperties.getRefreshTokenExpiresIn() / 1000);

        Cookie accessCookie = new Cookie(ACCESS_TOKEN_COOKIE, tokenPair.accessToken());
        accessCookie.setHttpOnly(true);
        accessCookie.setSecure(secure);
        accessCookie.setPath("/");
        accessCookie.setMaxAge(accessMaxAge);
        accessCookie.setAttribute("SameSite", "Strict");
        response.addCookie(accessCookie);

        Cookie refreshCookie = new Cookie(REFRESH_TOKEN_COOKIE, tokenPair.refreshToken());
        refreshCookie.setHttpOnly(true);
        refreshCookie.setSecure(secure);
        refreshCookie.setPath("/api/auth");
        refreshCookie.setMaxAge(refreshMaxAge);
        refreshCookie.setAttribute("SameSite", "Strict");
        response.addCookie(refreshCookie);
    }
}
