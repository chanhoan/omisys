package com.omisys.user.application.service;

import com.omisys.user.domain.model.User;
import com.omisys.user.domain.model.vo.UserRole;
import com.omisys.user.domain.repository.UserRepository;
import com.omisys.user.exception.UserErrorCode;
import com.omisys.user.exception.UserException;
import com.omisys.user_dto.infrastructure.UserDto;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserInternalServiceTest {

    @Mock
    private UserRepository userRepository;

    @Test
    void getUserByEmail_success() {
        User user = mock(User.class);
        when(user.getId()).thenReturn(1L);
        when(user.getUsername()).thenReturn("user1");
        when(user.getPassword()).thenReturn("encoded");
        when(user.getEmail()).thenReturn("user1@example.com");
        when(user.getRole()).thenReturn(UserRole.ROLE_USER);
        when(user.getPoint()).thenReturn(BigDecimal.TEN);
        when(userRepository.findByEmail("user1@example.com")).thenReturn(Optional.of(user));

        UserDto result = new UserInternalService(userRepository).getUserByEmail("user1@example.com");

        assertThat(result.getUserId()).isEqualTo(1L);
        assertThat(result.getUserName()).isEqualTo("user1");
        assertThat(result.getEmail()).isEqualTo("user1@example.com");
        assertThat(result.getRole()).isEqualTo("ROLE_USER");
    }

    @Test
    void getUserByEmail_notFound() {
        when(userRepository.findByEmail("missing@example.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> new UserInternalService(userRepository).getUserByEmail("missing@example.com"))
                .isInstanceOf(UserException.class)
                .satisfies(ex -> assertThat(((UserException) ex).getErrorCode())
                        .isEqualTo(UserErrorCode.USER_NOT_FOUND));
    }
}
