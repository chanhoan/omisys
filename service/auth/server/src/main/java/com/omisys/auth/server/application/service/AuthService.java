package com.omisys.auth.server.application.service;

import com.omisys.auth.server.application.dto.AuthResponse;
import com.omisys.auth.server.auth_dto.jwt.JwtClaim;
import com.omisys.auth.server.domain.RefreshToken;
import com.omisys.auth.server.exception.AuthErrorCode;
import com.omisys.auth.server.exception.AuthException;
import com.omisys.auth.server.infrastructure.properties.JwtProperties;
import com.omisys.auth.server.presentation.request.AuthRequest;
import com.omisys.user_dto.infrastructure.UserDto;
import io.jsonwebtoken.Jwts;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Map;

import static com.omisys.auth.server.domain.JwtConstant.*;

@Slf4j
@Service
public class AuthService {

    private final UserService userService;
    private final JwtProperties jwtProperties;
    private final SecretKey secretKey;
    private final PasswordEncoder passwordEncoder;
    private final RefreshTokenService refreshTokenService;

    public AuthService(UserService userService,
                       JwtProperties jwtProperties,
                       PasswordEncoder passwordEncoder,
                       RefreshTokenService refreshTokenService) {
        this.userService = userService;
        this.jwtProperties = jwtProperties;
        this.secretKey = createSecretKey();
        this.passwordEncoder = passwordEncoder;
        this.refreshTokenService = refreshTokenService;
    }

    public AuthResponse.TokenPair signIn(AuthRequest.SignIn request) {
        UserDto userData = userService.getUserByUsername(request.getUsername());

        if (userData == null || !passwordEncoder.matches(request.getPassword(), userData.getPassword())) {
            throw new AuthException(AuthErrorCode.SIGN_IN_FAIL);
        }

        JwtClaim jwtClaim = JwtClaim.create(userData.getUserId(), userData.getUserName(), userData.getRole());
        String accessToken = createAccessToken(jwtClaim);
        String refreshToken = refreshTokenService.createRefreshToken(
                userData.getUserId(), userData.getUserName(), userData.getRole());

        return new AuthResponse.TokenPair(accessToken, refreshToken);
    }

    public AuthResponse.TokenPair refresh(String oldRefreshToken) {
        RefreshToken newRefreshToken = refreshTokenService.rotateRefreshToken(oldRefreshToken);
        JwtClaim jwtClaim = JwtClaim.create(
                newRefreshToken.userId(), newRefreshToken.username(), newRefreshToken.role());
        String newAccessToken = createAccessToken(jwtClaim);
        return new AuthResponse.TokenPair(newAccessToken, newRefreshToken.tokenValue());
    }

    public void signOut(Long userId) {
        refreshTokenService.revokeAllByUserId(userId);
    }

    public void revokeRefreshToken(String refreshToken) {
        refreshTokenService.revokeByTokenValue(refreshToken);
    }

    private String createAccessToken(JwtClaim jwtClaim) {
        Map<String, Object> claims = Map.of(
                USER_ID, jwtClaim.getUserId(),
                USER_NAME, jwtClaim.getUsername(),
                USER_ROLE, jwtClaim.getRole());

        Date now = new Date(System.currentTimeMillis());
        long expiresIn = jwtProperties.getAccessTokenExpiresIn() * MILLI_SECOND;

        return Jwts.builder()
                .claims(claims)
                .issuedAt(now)
                .expiration(new Date(now.getTime() + expiresIn))
                .signWith(secretKey)
                .compact();
    }

    private SecretKey createSecretKey() {
        return new SecretKeySpec(
                jwtProperties.getSecretKey().getBytes(StandardCharsets.UTF_8),
                Jwts.SIG.HS256.key().build().getAlgorithm());
    }
}
