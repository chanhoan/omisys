package com.omisys.auth.server.infrastructure.repository;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.omisys.auth.server.domain.RefreshToken;
import com.omisys.auth.server.exception.AuthErrorCode;
import com.omisys.auth.server.exception.AuthException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;

@Slf4j
@Repository
@RequiredArgsConstructor
public class RedisRefreshTokenRepository implements RefreshTokenRepository {

    private static final String TOKEN_KEY_PREFIX = "rt:";
    private static final String USER_KEY_PREFIX = "rt:user:";

    /**
     * 원자적 RT Rotation Lua 스크립트.
     * KEYS[1] = 기존 토큰 키, KEYS[2] = 신규 토큰 키, KEYS[3] = 유저 Set 키
     * ARGV[1] = 기존 tokenValue, ARGV[2] = 신규 토큰 JSON, ARGV[3] = TTL(초), ARGV[4] = 신규 tokenValue
     * 반환값: 1(교체 성공), 0(기존 토큰 없음 — 재사용 또는 만료)
     */
    private static final DefaultRedisScript<Long> ROTATE_SCRIPT = new DefaultRedisScript<>("""
            local old = redis.call('GET', KEYS[1])
            if not old then return 0 end
            redis.call('DEL', KEYS[1])
            redis.call('SREM', KEYS[3], ARGV[1])
            redis.call('SET', KEYS[2], ARGV[2], 'EX', ARGV[3])
            redis.call('SADD', KEYS[3], ARGV[4])
            redis.call('EXPIRE', KEYS[3], ARGV[3])
            return 1
            """, Long.class);

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    @Override
    public void save(RefreshToken token, long ttlSeconds) {
        String json = serialize(token);
        redisTemplate.opsForValue().set(tokenKey(token.tokenValue()), json, ttlSeconds, TimeUnit.SECONDS);
        redisTemplate.opsForSet().add(userKey(token.userId()), token.tokenValue());
        redisTemplate.expire(userKey(token.userId()), ttlSeconds, TimeUnit.SECONDS);
    }

    @Override
    public Optional<RefreshToken> findByTokenValue(String tokenValue) {
        String json = redisTemplate.opsForValue().get(tokenKey(tokenValue));
        if (json == null) {
            return Optional.empty();
        }
        return Optional.of(deserialize(json, tokenValue));
    }

    @Override
    public boolean atomicRotate(String oldTokenValue, RefreshToken newToken, long ttlSeconds) {
        String newJson = serialize(newToken);
        List<String> keys = List.of(
                tokenKey(oldTokenValue),
                tokenKey(newToken.tokenValue()),
                userKey(newToken.userId())
        );
        Long result = redisTemplate.execute(ROTATE_SCRIPT, keys,
                oldTokenValue,
                newJson,
                String.valueOf(ttlSeconds),
                newToken.tokenValue()
        );
        return Long.valueOf(1L).equals(result);
    }

    @Override
    public void deleteByTokenValue(String tokenValue) {
        findByTokenValue(tokenValue).ifPresent(rt -> {
            List<String> keys = List.of(tokenKey(tokenValue));
            redisTemplate.delete(keys);
            redisTemplate.opsForSet().remove(userKey(rt.userId()), tokenValue);
        });
    }

    @Override
    public void deleteAllByUserId(Long userId) {
        String uk = userKey(userId);
        Set<String> tokenValues = redisTemplate.opsForSet().members(uk);
        List<String> toDelete = new ArrayList<>();
        if (tokenValues != null) {
            tokenValues.forEach(tv -> toDelete.add(tokenKey(tv)));
        }
        toDelete.add(uk);
        redisTemplate.delete(toDelete);
    }

    private String serialize(RefreshToken token) {
        try {
            return objectMapper.writeValueAsString(token);
        } catch (JsonProcessingException e) {
            log.error("RefreshToken 직렬화 실패: userId={}", token.userId(), e);
            throw new AuthException(AuthErrorCode.INTERNAL_SERVER_ERROR);
        }
    }

    private RefreshToken deserialize(String json, String tokenValue) {
        try {
            return objectMapper.readValue(json, RefreshToken.class);
        } catch (JsonProcessingException e) {
            log.error("RefreshToken 역직렬화 실패 — Redis 데이터 손상 가능성: tokenValue={}", tokenValue, e);
            throw new AuthException(AuthErrorCode.INTERNAL_SERVER_ERROR);
        }
    }

    private String tokenKey(String tokenValue) {
        return TOKEN_KEY_PREFIX + tokenValue;
    }

    private String userKey(Long userId) {
        return USER_KEY_PREFIX + userId;
    }
}
