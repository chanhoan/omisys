package com.omisys.auth.server.application.service;

import com.omisys.auth.server.auth_dto.jwt.JwtClaim;
import com.omisys.auth.server.exception.AuthErrorCode;
import com.omisys.auth.server.exception.AuthException;
import com.omisys.auth.server.infrastructure.properties.JwtProperties;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

import static com.omisys.auth.server.domain.JwtConstant.*;
import static org.assertj.core.api.Assertions.*;

class AuthInternalServiceTest {

    /**
     * HS256 서명용 SecretKey는 최소 32바이트 이상이 안전합니다.
     * (너무 짧으면 jjwt에서 예외가 날 수 있음)
     */
    private static final String SECRET = "test-secret-key-test-secret-key-test-secret-key";

    private AuthInternalService newService() {
        JwtProperties props = new JwtProperties();
        props.setSecretKey(SECRET);
        props.setAccessTokenExpiresIn(60_000);
        return new AuthInternalService(props);
    }

    @Test
    @DisplayName("verifyToken 성공: 정상 토큰이면 JwtClaim(userId/userName/role)로 변환된다")
    void verifyToken_success_returns_jwtClaim() {
        // given
        AuthInternalService service = newService();

        SecretKey key = Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8));

        String token = Jwts.builder()
                // AuthInternalService.convert()가 아래 키로 클레임을 읽는다.
                .claim(USER_ID, 1L)
                .claim(USER_NAME, "user1")
                .claim(USER_ROLE, "ROLE_USER")
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + 60_000))
                .signWith(key)
                .compact();

        // when
        JwtClaim claim = service.verifyToken(token);

        // then
        assertThat(claim).isNotNull();
        assertThat(claim.getUserId()).isEqualTo(1L);
        assertThat(claim.getUsername()).isEqualTo("user1");
        assertThat(claim.getRole()).isEqualTo("ROLE_USER");
    }

    @Test
    @DisplayName("verifyToken 실패: 만료 토큰이면 AuthException(TOEKN_EXPIRED)")
    void verifyToken_fail_expired_throws_tokenExpired() {
        // given
        AuthInternalService service = newService();

        SecretKey key = Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8));

        String expired = Jwts.builder()
                .claim(USER_ID, 1L)
                .claim(USER_NAME, "user1")
                .claim(USER_ROLE, "ROLE_USER")
                .issuedAt(new Date(System.currentTimeMillis() - 120_000))
                .expiration(new Date(System.currentTimeMillis() - 60_000)) // 이미 만료
                .signWith(key)
                .compact();

        // when & then
        assertThatThrownBy(() -> service.verifyToken(expired))
                .isInstanceOf(AuthException.class)
                .satisfies(ex -> {
                    AuthException ae = (AuthException) ex;
                    assertThat(ae.getErrorCode()).isEqualTo(AuthErrorCode.TOEKN_EXPIRED);
                });
    }

    @Test
    @DisplayName("verifyToken 실패: 위변조(다른 키로 서명)면 AuthException(INVALID_TOKEN)")
    void verifyToken_fail_tampered_signature_throws_invalidToken() {
        // given
        AuthInternalService service = newService();

        SecretKey otherKey = Keys.hmacShaKeyFor(
                "other-secret-key-other-secret-key-other-secret-key".getBytes(StandardCharsets.UTF_8)
        );

        String token = Jwts.builder()
                .claim(USER_ID, 1L)
                .claim(USER_NAME, "user1")
                .claim(USER_ROLE, "ROLE_USER")
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + 60_000))
                .signWith(otherKey) // 다른 키로 서명 → 검증 실패
                .compact();

        // when & then
        assertThatThrownBy(() -> service.verifyToken(token))
                .isInstanceOf(AuthException.class)
                .satisfies(ex -> {
                    AuthException ae = (AuthException) ex;
                    assertThat(ae.getErrorCode()).isEqualTo(AuthErrorCode.INVALID_TOKEN);
                });
    }

    @Test
    @DisplayName("verifyToken 실패: 형식이 깨진 토큰이면 AuthException(INVALID_TOKEN)")
    void verifyToken_fail_malformed_throws_invalidToken() {
        // given
        AuthInternalService service = newService();
        String malformed = "this.is.not.jwt";

        // when & then
        assertThatThrownBy(() -> service.verifyToken(malformed))
                .isInstanceOf(AuthException.class)
                .satisfies(ex -> {
                    AuthException ae = (AuthException) ex;
                    assertThat(ae.getErrorCode()).isEqualTo(AuthErrorCode.INVALID_TOKEN);
                });
    }
}
