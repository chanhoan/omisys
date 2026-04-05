package com.omisys.auth.server.application.service;

import com.omisys.auth.server.domain.RefreshToken;
import com.omisys.auth.server.exception.AuthErrorCode;
import com.omisys.auth.server.exception.AuthException;
import com.omisys.auth.server.infrastructure.properties.JwtProperties;
import com.omisys.auth.server.infrastructure.repository.RefreshTokenRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Slf4j
@Service
public class RefreshTokenService {

    private static final long MILLI_TO_SECOND = 1000L;

    private final RefreshTokenRepository refreshTokenRepository;
    private final long ttlSeconds;

    @org.springframework.beans.factory.annotation.Autowired
    public RefreshTokenService(RefreshTokenRepository refreshTokenRepository, JwtProperties jwtProperties) {
        this.refreshTokenRepository = refreshTokenRepository;
        this.ttlSeconds = jwtProperties.getRefreshTokenExpiresIn() / MILLI_TO_SECOND;
    }

    RefreshTokenService(RefreshTokenRepository refreshTokenRepository, long ttlSeconds) {
        this.refreshTokenRepository = refreshTokenRepository;
        this.ttlSeconds = ttlSeconds;
    }

    public String createRefreshToken(Long userId, String username, String role) {
        String tokenValue = UUID.randomUUID().toString();
        String familyId = UUID.randomUUID().toString();
        RefreshToken token = new RefreshToken(tokenValue, userId, username, role, familyId);
        refreshTokenRepository.save(token, ttlSeconds);
        return tokenValue;
    }

    public RefreshToken rotateRefreshToken(String oldTokenValue) {
        RefreshToken existing = refreshTokenRepository.findByTokenValue(oldTokenValue)
                .orElseThrow(() -> {
                    log.warn("[RT-REUSE] 존재하지 않는 RT 사용 시도: {}", oldTokenValue);
                    return new AuthException(AuthErrorCode.REFRESH_TOKEN_REUSE_DETECTED);
                });

        String newTokenValue = UUID.randomUUID().toString();
        RefreshToken newToken = new RefreshToken(
                newTokenValue, existing.userId(), existing.username(), existing.role(), existing.familyId());

        boolean rotated = refreshTokenRepository.atomicRotate(oldTokenValue, newToken, ttlSeconds);
        if (!rotated) {
            log.warn("[RT-REUSE] 원자적 교체 실패 — 동시 재사용 감지: {}", oldTokenValue);
            throw new AuthException(AuthErrorCode.REFRESH_TOKEN_REUSE_DETECTED);
        }

        return newToken;
    }

    public void revokeByTokenValue(String tokenValue) {
        refreshTokenRepository.deleteByTokenValue(tokenValue);
    }

    public void revokeAllByUserId(Long userId) {
        refreshTokenRepository.deleteAllByUserId(userId);
    }
}
