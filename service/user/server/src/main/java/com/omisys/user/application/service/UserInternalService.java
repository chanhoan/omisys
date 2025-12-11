package com.omisys.user.application.service;

import com.omisys.user.domain.model.User;
import com.omisys.user.domain.repository.UserRepository;
import com.omisys.user.exception.UserException;
import com.omisys.user_dto.infrastructure.UserDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import static com.omisys.user.exception.UserErrorCode.USER_NOT_FOUND;

@Service
@RequiredArgsConstructor
public class UserInternalService {

    private final UserRepository userRepository;

    public UserDto getUserByUsername(String username) {

        User user = userRepository
                .findByUsername(username)
                .orElseThrow(() -> new UserException(USER_NOT_FOUND));

        return new UserDto(
                user.getId(),
                user.getUsername(),
                user.getPassword(),
                user.getEmail(),
                user.getRole().name(),
                user.getPoint()
        );

    }

    public UserDto getUserByUserId(Long userId) {

        User user = userRepository
                .findById(userId)
                .orElseThrow(() -> new UserException(USER_NOT_FOUND));

        return new UserDto(
                user.getId(),
                user.getUsername(),
                user.getPassword(),
                user.getEmail(),
                user.getRole().name(),
                user.getPoint()
        );

    }
}
