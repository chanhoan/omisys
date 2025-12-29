package com.omisys.auth.server.application.service;

import com.omisys.auth.server.application.dto.AuthResponse;
import com.omisys.auth.server.exception.AuthErrorCode;
import com.omisys.auth.server.exception.AuthException;
import com.omisys.auth.server.infrastructure.properties.JwtProperties;
import com.omisys.auth.server.presentation.request.AuthRequest;
import com.omisys.user_dto.infrastructure.UserDto;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;

import static com.omisys.auth.server.domain.JwtConstant.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock private UserService userService;
    @Mock private PasswordEncoder passwordEncoder;

    /**
     * AuthService는 생성자에서 JwtProperties를 받아 SecretKey를 만들기 때문에
     * @InjectMocks 대신 "직접 new"로 만드는 것이 테스트 안정성이 좋습니다.
     */
    private AuthService newAuthService() {
        JwtProperties props = new JwtProperties();
        // HS256용 secret은 충분히 길어야 함(32바이트 이상 권장)
        props.setSecretKey("test-secret-key-test-secret-key-test-secret-key");
        props.setAccessTokenExpiresIn(60000);
        return new AuthService(userService, props, passwordEncoder);
    }

    @Test
    @DisplayName("signIn 성공: matches 통과 → JWT 토큰 발급(클레임 포함)")
    void signIn_success() {
        // given
        AuthService authService = newAuthService();

        AuthRequest.SignIn request = new AuthRequest.SignIn("user1", "pw");

        UserDto userDto = mock(UserDto.class);
        when(userDto.getPassword()).thenReturn("ENCODED");
        when(userDto.getUserId()).thenReturn(1L);
        when(userDto.getUserName()).thenReturn("user1");
        when(userDto.getRole()).thenReturn("ROLE_USER");

        when(userService.getUserByUsername("user1")).thenReturn(userDto);
        when(passwordEncoder.matches("pw", "ENCODED")).thenReturn(true);

        // when
        AuthResponse.SignIn response = authService.signIn(request);

        // then
        assertThat(response.getToken()).isNotBlank();

        // 토큰이 "정말로" userId/userName/role 클레임을 들고 있는지 검증
        SecretKey key = Keys.hmacShaKeyFor("test-secret-key-test-secret-key-test-secret-key".getBytes(StandardCharsets.UTF_8));

        Claims claims = Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(response.getToken())
                .getPayload();

        assertThat(claims.get(USER_ID)).isEqualTo(1);      // JSON number로 들어가면 Integer로 파싱될 수 있음
        assertThat(claims.get(USER_NAME)).isEqualTo("user1");
        assertThat(claims.get(USER_ROLE)).isEqualTo("ROLE_USER");
    }

    @Test
    @DisplayName("signIn 실패: user가 null이면 SIGN_IN_FAIL")
    void signIn_fail_user_null() {
        // given
        AuthService authService = newAuthService();
        when(userService.getUserByUsername("user1")).thenReturn(null);

        // when & then
        assertThatThrownBy(() -> authService.signIn(new AuthRequest.SignIn("user1", "pw")))
                .isInstanceOf(AuthException.class)
                .satisfies(ex -> {
                    AuthException ae = (AuthException) ex;
                    assertThat(ae.getErrorCode()).isEqualTo(AuthErrorCode.SIGN_IN_FAIL);
                });
    }

    @Test
    @DisplayName("signIn 실패: password mismatch면 SIGN_IN_FAIL")
    void signIn_fail_password_mismatch() {
        // given
        AuthService authService = newAuthService();

        UserDto userDto = mock(UserDto.class);
        when(userDto.getPassword()).thenReturn("ENCODED");
        when(userService.getUserByUsername("user1")).thenReturn(userDto);

        when(passwordEncoder.matches("pw", "ENCODED")).thenReturn(false);

        // when & then
        assertThatThrownBy(() -> authService.signIn(new AuthRequest.SignIn("user1", "pw")))
                .isInstanceOf(AuthException.class)
                .satisfies(ex -> {
                    AuthException ae = (AuthException) ex;
                    assertThat(ae.getErrorCode()).isEqualTo(AuthErrorCode.SIGN_IN_FAIL);
                });
    }
}
