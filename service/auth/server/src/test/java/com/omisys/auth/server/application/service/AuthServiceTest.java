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
import org.junit.jupiter.api.BeforeEach;
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
    @Mock private RefreshTokenService refreshTokenService;

    private static final String SECRET = "test-secret-key-test-secret-key-test-secret-key";

    private AuthService newAuthService() {
        JwtProperties props = new JwtProperties();
        props.setSecretKey(SECRET);
        props.setAccessTokenExpiresIn(60000);
        props.setRefreshTokenExpiresIn(604800000);
        return new AuthService(userService, props, passwordEncoder, refreshTokenService);
    }

    @Test
    @DisplayName("signIn 성공: matches 통과 → AT + RT 발급")
    void signIn_success() {
        // given
        AuthService authService = newAuthService();
        when(refreshTokenService.createRefreshToken(1L, "user1", "ROLE_USER")).thenReturn("refresh-token-value");

        UserDto userDto = mock(UserDto.class);
        when(userDto.getPassword()).thenReturn("ENCODED");
        when(userDto.getUserId()).thenReturn(1L);
        when(userDto.getUserName()).thenReturn("user1");
        when(userDto.getRole()).thenReturn("ROLE_USER");

        when(userService.getUserByUsername("user1")).thenReturn(userDto);
        when(passwordEncoder.matches("pw", "ENCODED")).thenReturn(true);

        // when
        AuthResponse.TokenPair response = authService.signIn(new AuthRequest.SignIn("user1", "pw"));

        // then
        assertThat(response.accessToken()).isNotBlank();
        assertThat(response.refreshToken()).isEqualTo("refresh-token-value");

        // AT 클레임 검증
        SecretKey key = Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8));
        Claims claims = Jwts.parser().verifyWith(key).build()
                .parseSignedClaims(response.accessToken()).getPayload();

        assertThat(claims.get(USER_ID)).isEqualTo(1);
        assertThat(claims.get(USER_NAME)).isEqualTo("user1");
        assertThat(claims.get(USER_ROLE)).isEqualTo("ROLE_USER");

        verify(refreshTokenService).createRefreshToken(1L, "user1", "ROLE_USER");
    }

    @Test
    @DisplayName("signIn 실패: user가 null이면 SIGN_IN_FAIL")
    void signIn_fail_user_null() {
        AuthService authService = newAuthService();
        when(userService.getUserByUsername("user1")).thenReturn(null);

        assertThatThrownBy(() -> authService.signIn(new AuthRequest.SignIn("user1", "pw")))
                .isInstanceOf(AuthException.class)
                .satisfies(ex -> assertThat(((AuthException) ex).getErrorCode())
                        .isEqualTo(AuthErrorCode.SIGN_IN_FAIL));
    }

    @Test
    @DisplayName("signIn 실패: password mismatch면 SIGN_IN_FAIL")
    void signIn_fail_password_mismatch() {
        AuthService authService = newAuthService();

        UserDto userDto = mock(UserDto.class);
        when(userDto.getPassword()).thenReturn("ENCODED");
        when(userService.getUserByUsername("user1")).thenReturn(userDto);
        when(passwordEncoder.matches("pw", "ENCODED")).thenReturn(false);

        assertThatThrownBy(() -> authService.signIn(new AuthRequest.SignIn("user1", "pw")))
                .isInstanceOf(AuthException.class)
                .satisfies(ex -> assertThat(((AuthException) ex).getErrorCode())
                        .isEqualTo(AuthErrorCode.SIGN_IN_FAIL));
    }

    @Test
    @DisplayName("refresh 성공: 유효한 RT → 새 AT + 새 RT 발급")
    void refresh_success() {
        // given
        AuthService authService = newAuthService();
        com.omisys.auth.server.domain.RefreshToken newRt =
                new com.omisys.auth.server.domain.RefreshToken("new-rt", 1L, "user1", "ROLE_USER", "fam-1");
        when(refreshTokenService.rotateRefreshToken("old-rt")).thenReturn(newRt);

        // when
        AuthResponse.TokenPair response = authService.refresh("old-rt");

        // then
        assertThat(response.accessToken()).isNotBlank();
        assertThat(response.refreshToken()).isEqualTo("new-rt");
        verify(refreshTokenService).rotateRefreshToken("old-rt");
    }

    @Test
    @DisplayName("signOut 성공: userId로 RT 전체 무효화")
    void signOut_success() {
        // given
        AuthService authService = newAuthService();

        // when
        authService.signOut(1L);

        // then
        verify(refreshTokenService).revokeAllByUserId(1L);
    }
}
