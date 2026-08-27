package com.omisys.user.application.service;

import com.omisys.user.domain.model.PointHistory;
import com.omisys.user.domain.model.User;
import com.omisys.user.domain.model.vo.UserRole;
import com.omisys.user.domain.repository.PointHistoryRepository;
import com.omisys.user.domain.repository.UserRepository;
import com.omisys.user.exception.UserErrorCode;
import com.omisys.user.exception.UserException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PointHistoryServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private PointHistoryRepository pointHistoryRepository;

    @InjectMocks private PointHistoryService pointHistoryService;

    @Test
    @DisplayName("deletePointHistory 성공(ROLE_ADMIN): 본인 아니어도 삭제 가능")
    void deletePointHistory_success_admin_can_delete_any() {
        // given
        long userId = 1L;
        long pointHistoryId = 10L;

        User admin = mock(User.class);
        when(admin.getRole()).thenReturn(UserRole.ROLE_ADMIN);
        when(userRepository.findById(userId)).thenReturn(Optional.of(admin));

        PointHistory history = mock(PointHistory.class);
        when(pointHistoryRepository.findById(pointHistoryId)).thenReturn(Optional.of(history));

        // when
        pointHistoryService.deletePointHistory(pointHistoryId, userId);

        // then
        verify(pointHistoryRepository).delete(history);
    }

    @Test
    @DisplayName("deletePointHistory 실패(ROLE_USER): 본인 포인트 내역 아니면 INVAILD_POINT_USER 예외")
    void deletePointHistory_fail_user_not_owner() {
        // given
        long userId = 1L;
        long pointHistoryId = 10L;

        User user = mock(User.class);
        when(user.getRole()).thenReturn(UserRole.ROLE_USER);
        when(user.getId()).thenReturn(userId);
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        // 포인트 내역의 userId가 다르게 설정되면 “내 것 아님” 케이스
        User other = mock(User.class);
        when(other.getId()).thenReturn(999L);

        PointHistory history = mock(PointHistory.class);
        when(history.getUser()).thenReturn(other);
        when(pointHistoryRepository.findById(pointHistoryId)).thenReturn(Optional.of(history));

        // when & then
        assertThatThrownBy(() -> pointHistoryService.deletePointHistory(pointHistoryId, userId))
                .isInstanceOf(UserException.class)
                .satisfies(ex -> {
                    UserException ue = (UserException) ex;
                    assertThat(ue.getErrorCode()).isEqualTo(UserErrorCode.INVAILD_POINT_USER);
                });

        verify(pointHistoryRepository, never()).delete(any());
    }
}
