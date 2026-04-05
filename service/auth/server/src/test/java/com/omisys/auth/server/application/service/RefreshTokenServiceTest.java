package com.omisys.auth.server.application.service;

import com.omisys.auth.server.domain.RefreshToken;
import com.omisys.auth.server.exception.AuthErrorCode;
import com.omisys.auth.server.exception.AuthException;
import com.omisys.auth.server.infrastructure.repository.RefreshTokenRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RefreshTokenServiceTest {

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    private RefreshTokenService refreshTokenService;

    private static final long TTL_SECONDS = 604800L;

    @BeforeEach
    void setUp() {
        refreshTokenService = new RefreshTokenService(refreshTokenRepository, TTL_SECONDS);
    }

    @Test
    @DisplayName("createRefreshToken: userId/username/role로 RT 생성 후 저장, tokenValue 반환")
    void createRefreshToken_success() {
        // when
        String tokenValue = refreshTokenService.createRefreshToken(1L, "user1", "ROLE_USER");

        // then
        assertThat(tokenValue).isNotBlank();

        ArgumentCaptor<RefreshToken> captor = ArgumentCaptor.forClass(RefreshToken.class);
        verify(refreshTokenRepository).save(captor.capture(), eq(TTL_SECONDS));

        RefreshToken saved = captor.getValue();
        assertThat(saved.tokenValue()).isEqualTo(tokenValue);
        assertThat(saved.userId()).isEqualTo(1L);
        assertThat(saved.username()).isEqualTo("user1");
        assertThat(saved.role()).isEqualTo("ROLE_USER");
        assertThat(saved.familyId()).isNotBlank();
    }

    @Test
    @DisplayName("rotateRefreshToken: 유효한 RT → atomicRotate 성공, 새 RT 반환, 사용자 정보 유지")
    void rotateRefreshToken_success() {
        // given
        String oldToken = "old-token-uuid";
        RefreshToken stored = new RefreshToken(oldToken, 1L, "user1", "ROLE_USER", "family-uuid");
        when(refreshTokenRepository.findByTokenValue(oldToken)).thenReturn(Optional.of(stored));
        when(refreshTokenRepository.atomicRotate(eq(oldToken), any(RefreshToken.class), eq(TTL_SECONDS)))
                .thenReturn(true);

        // when
        RefreshToken newToken = refreshTokenService.rotateRefreshToken(oldToken);

        // then
        assertThat(newToken.tokenValue()).isNotBlank().isNotEqualTo(oldToken);
        assertThat(newToken.userId()).isEqualTo(1L);
        assertThat(newToken.username()).isEqualTo("user1");
        assertThat(newToken.role()).isEqualTo("ROLE_USER");
        assertThat(newToken.familyId()).isEqualTo("family-uuid");

        // atomicRotate가 호출됐는지, 새 토큰의 tokenValue가 일치하는지 검증
        ArgumentCaptor<RefreshToken> captor = ArgumentCaptor.forClass(RefreshToken.class);
        verify(refreshTokenRepository).atomicRotate(eq(oldToken), captor.capture(), eq(TTL_SECONDS));
        assertThat(captor.getValue().tokenValue()).isEqualTo(newToken.tokenValue());

        // 직접 save/delete는 호출되지 않아야 함 (atomicRotate가 담당)
        verify(refreshTokenRepository, never()).save(any(), anyLong());
        verify(refreshTokenRepository, never()).deleteByTokenValue(anyString());
    }

    @Test
    @DisplayName("rotateRefreshToken: atomicRotate 실패(동시 재사용) → REFRESH_TOKEN_REUSE_DETECTED")
    void rotateRefreshToken_atomicRotateFail_throwsReuseDetected() {
        // given
        String oldToken = "old-token-uuid";
        RefreshToken stored = new RefreshToken(oldToken, 1L, "user1", "ROLE_USER", "family-uuid");
        when(refreshTokenRepository.findByTokenValue(oldToken)).thenReturn(Optional.of(stored));
        when(refreshTokenRepository.atomicRotate(eq(oldToken), any(RefreshToken.class), eq(TTL_SECONDS)))
                .thenReturn(false);

        // when & then
        assertThatThrownBy(() -> refreshTokenService.rotateRefreshToken(oldToken))
                .isInstanceOf(AuthException.class)
                .satisfies(ex -> assertThat(((AuthException) ex).getErrorCode())
                        .isEqualTo(AuthErrorCode.REFRESH_TOKEN_REUSE_DETECTED));
    }

    @Test
    @DisplayName("rotateRefreshToken: 존재하지 않는 RT → REFRESH_TOKEN_REUSE_DETECTED 예외")
    void rotateRefreshToken_tokenNotFound_throwsReuseDetected() {
        // given
        when(refreshTokenRepository.findByTokenValue("ghost-token")).thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> refreshTokenService.rotateRefreshToken("ghost-token"))
                .isInstanceOf(AuthException.class)
                .satisfies(ex -> assertThat(((AuthException) ex).getErrorCode())
                        .isEqualTo(AuthErrorCode.REFRESH_TOKEN_REUSE_DETECTED));

        verify(refreshTokenRepository, never()).atomicRotate(any(), any(), anyLong());
    }

    @Test
    @DisplayName("revokeByTokenValue: 특정 RT 개별 폐기")
    void revokeByTokenValue_success() {
        // when
        refreshTokenService.revokeByTokenValue("some-token");

        // then
        verify(refreshTokenRepository).deleteByTokenValue("some-token");
    }

    @Test
    @DisplayName("revokeAllByUserId: userId의 모든 RT 삭제")
    void revokeAllByUserId_success() {
        // when
        refreshTokenService.revokeAllByUserId(1L);

        // then
        verify(refreshTokenRepository).deleteAllByUserId(1L);
    }
}
