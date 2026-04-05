package com.omisys.auth.server.presentation.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.omisys.auth.server.application.dto.AuthResponse;
import com.omisys.auth.server.application.service.AuthService;
import com.omisys.auth.server.auth_dto.jwt.JwtClaim;
import com.omisys.auth.server.exception.AuthErrorCode;
import com.omisys.auth.server.exception.AuthException;
import com.omisys.auth.server.infrastructure.properties.JwtProperties;
import com.omisys.auth.server.presentation.request.AuthRequest;
import com.omisys.common.domain.response.ApiResponse;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

@Slf4j
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private static final String ACCESS_TOKEN_COOKIE = "accessToken";
    private static final String REFRESH_TOKEN_COOKIE = "refreshToken";

    private final AuthService authService;
    private final JwtProperties jwtProperties;
    private final ObjectMapper objectMapper;

    @PostMapping("/sign-in")
    public ApiResponse<Void> signIn(@RequestBody AuthRequest.SignIn request,
                                    HttpServletResponse response) {
        AuthResponse.TokenPair tokenPair = authService.signIn(request);
        setTokenCookies(response, tokenPair);
        return ApiResponse.ok(null);
    }

    @PostMapping("/refresh")
    public ApiResponse<Void> refresh(
            @CookieValue(name = REFRESH_TOKEN_COOKIE, required = false) String refreshToken,
            HttpServletResponse response) {
        if (refreshToken == null || refreshToken.isBlank()) {
            throw new AuthException(AuthErrorCode.REFRESH_TOKEN_NOT_FOUND);
        }
        AuthResponse.TokenPair tokenPair = authService.refresh(refreshToken);
        setTokenCookies(response, tokenPair);
        return ApiResponse.ok(null);
    }

    @PostMapping("/sign-out")
    public ApiResponse<Void> signOut(
            @RequestHeader(value = "X-User-Claims", required = false) String userClaimsHeader,
            @CookieValue(name = REFRESH_TOKEN_COOKIE, required = false) String refreshToken,
            HttpServletResponse response) {
        Long userId = extractUserId(userClaimsHeader);
        if (userId != null) {
            authService.signOut(userId);
        } else if (refreshToken != null && !refreshToken.isBlank()) {
            log.warn("sign-out: X-User-Claims 없음 — RT 기반 개별 폐기로 대체");
            authService.revokeRefreshToken(refreshToken);
        } else {
            log.warn("sign-out: 사용자 식별 정보 없음 — RT 폐기 불가");
        }
        clearTokenCookies(response);
        return ApiResponse.ok(null);
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

    private void clearTokenCookies(HttpServletResponse response) {
        Cookie accessCookie = new Cookie(ACCESS_TOKEN_COOKIE, "");
        accessCookie.setHttpOnly(true);
        accessCookie.setPath("/");
        accessCookie.setMaxAge(0);
        accessCookie.setAttribute("SameSite", "Strict");
        response.addCookie(accessCookie);

        Cookie refreshCookie = new Cookie(REFRESH_TOKEN_COOKIE, "");
        refreshCookie.setHttpOnly(true);
        refreshCookie.setPath("/api/auth");
        refreshCookie.setMaxAge(0);
        refreshCookie.setAttribute("SameSite", "Strict");
        response.addCookie(refreshCookie);
    }

    private Long extractUserId(String userClaimsHeader) {
        if (userClaimsHeader == null) return null;
        try {
            String decoded = URLDecoder.decode(userClaimsHeader, StandardCharsets.UTF_8);
            JwtClaim claim = objectMapper.readValue(decoded, JwtClaim.class);
            return claim.getUserId();
        } catch (Exception e) {
            log.warn("X-User-Claims 파싱 실패: {}", e.getMessage());
            return null;
        }
    }
}
