package com.omisys.auth.server.infrastructure.repository;

import com.omisys.auth.server.domain.RefreshToken;

import java.util.Optional;

public interface RefreshTokenRepository {

    void save(RefreshToken token, long ttlSeconds);

    Optional<RefreshToken> findByTokenValue(String tokenValue);

    /**
     * oldTokenValue 존재 여부를 확인하고, 존재할 때만 삭제 후 newToken을 저장하는 원자적 연산.
     * @return true: 정상 교체, false: oldToken이 이미 존재하지 않음 (재사용 감지 또는 만료)
     */
    boolean atomicRotate(String oldTokenValue, RefreshToken newToken, long ttlSeconds);

    void deleteByTokenValue(String tokenValue);

    void deleteAllByUserId(Long userId);
}
