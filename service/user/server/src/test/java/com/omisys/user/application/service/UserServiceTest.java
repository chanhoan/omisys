package com.omisys.user.application.service;

import com.omisys.user.domain.model.Tier;
import com.omisys.user.domain.model.User;
import com.omisys.user.domain.repository.TierRepository;
import com.omisys.user.domain.repository.UserRepository;
import com.omisys.user.domain.repository.UserTierRepository;
import com.omisys.user.exception.UserErrorCode;
import com.omisys.user.exception.UserException;
import com.omisys.user.presentation.request.UserRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private TierRepository tierRepository;
    @Mock private UserTierRepository userTierRepository;
    @Mock private PasswordEncoder passwordEncoder;

    @InjectMocks private UserService userService;

    @Test
    @DisplayName("createUser 성공: username 중복없음 → 비밀번호 인코딩 → User 저장 → 기본Tier(아이언) 조회 → UserTier 저장")
    void createUser_success_saves_user_and_userTier() {
        // given
        UserRequest.Create request = new UserRequest.Create(
                "user1",
                "Password1!",
                "user1@omisys.com",
                "nick",
                com.omisys.user.domain.model.vo.UserRole.ROLE_USER
        );

        when(userRepository.findByUsername("user1")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("Password1!")).thenReturn("ENCODED");

        // Tier는 내부에서 값만 들고 흐르므로 mock이어도 충분
        Tier ironTier = mock(Tier.class);
        when(tierRepository.findByName("아이언")).thenReturn(Optional.of(ironTier));

        // when
        userService.createUser(request);

        // then
        // 1) 유저 저장 호출 검증 + 저장되는 password가 인코딩 결과인지 확인
        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        User savedUser = userCaptor.getValue();

        // create()로 생성된 User가 인코딩된 비밀번호를 들고 저장되는지 확인
        assertThat(savedUser.getUsername()).isEqualTo("user1");
        assertThat(savedUser.getPassword()).isEqualTo("ENCODED");

        // 2) 기본 등급 조회/저장 호출 검증
        verify(tierRepository).findByName("아이언");
        verify(userTierRepository).save(any());

        // 3) 인코더 호출이 실제로 일어났는지 확인
        verify(passwordEncoder).encode("Password1!");
    }

    @Test
    @DisplayName("createUser 실패: username 중복이면 USER_CONFLICT 예외")
    void createUser_fail_duplicate_username() {
        // given
        UserRequest.Create request = new UserRequest.Create(
                "dupuser",
                "Password1!",
                "dup@omisys.com",
                "nick",
                com.omisys.user.domain.model.vo.UserRole.ROLE_USER
        );

        when(userRepository.findByUsername("dupuser")).thenReturn(Optional.of(mock(User.class)));

        // when & then
        assertThatThrownBy(() -> userService.createUser(request))
                .isInstanceOf(UserException.class)
                .satisfies(ex -> {
                    UserException ue = (UserException) ex;
                    assertThat(ue.getErrorCode()).isEqualTo(UserErrorCode.USER_CONFLICT);
                });

        // 중복에서 끊기므로 아래는 호출되면 안 됨
        verify(passwordEncoder, never()).encode(anyString());
        verify(userRepository, never()).save(any(User.class));
        verify(userTierRepository, never()).save(any());
    }

    @Test
    @DisplayName("updateUserPassword 성공: currentPassword matches → 새 비밀번호 encode → user.updatePassword 호출")
    void updateUserPassword_success() {
        // given
        long userId = 1L;

        // User는 엔티티이지만 여기서는 updatePassword 호출 여부가 핵심이라 mock이 가장 안전함
        User user = mock(User.class);
        when(user.getPassword()).thenReturn("ENCODED_OLD");
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        UserRequest.UpdatePassword request = new UserRequest.UpdatePassword("OldPass1!", "NewPass1!");
        when(passwordEncoder.matches("OldPass1!", "ENCODED_OLD")).thenReturn(true);
        when(passwordEncoder.encode("NewPass1!")).thenReturn("ENCODED_NEW");

        // when
        userService.updateUserPassword(userId, request);

        // then
        verify(user).updatePassword("ENCODED_NEW");
        verify(passwordEncoder).matches("OldPass1!", "ENCODED_OLD");
        verify(passwordEncoder).encode("NewPass1!");
    }

    @Test
    @DisplayName("updateUserPassword 실패: currentPassword 불일치면 INVAILD_PASSWORD 예외")
    void updateUserPassword_fail_invalid_password() {
        // given
        long userId = 1L;

        User user = mock(User.class);
        when(user.getPassword()).thenReturn("ENCODED_OLD");
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        UserRequest.UpdatePassword request = new UserRequest.UpdatePassword("WrongPass1!", "NewPass1!");
        when(passwordEncoder.matches("WrongPass1!", "ENCODED_OLD")).thenReturn(false);

        // when & then
        assertThatThrownBy(() -> userService.updateUserPassword(userId, request))
                .isInstanceOf(UserException.class)
                .satisfies(ex -> {
                    UserException ue = (UserException) ex;
                    assertThat(ue.getErrorCode()).isEqualTo(UserErrorCode.INVAILD_PASSWORD);
                });

        // 실패 시 updatePassword 호출되면 안 됨
        verify(user, never()).updatePassword(anyString());
        verify(passwordEncoder, never()).encode(anyString());
    }

    @Test
    @DisplayName("deleteUser 성공: 사용자 조회 후 isDeleted=true 반영")
    void deleteUser_success() {
        // given
        long userId = 1L;

        User user = mock(User.class);
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        // when
        userService.deleteUser(userId);

        // then
        verify(user).delete(true);
    }
}
