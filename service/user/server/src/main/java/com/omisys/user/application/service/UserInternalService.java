package com.omisys.user.application.service;

import com.omisys.user.domain.model.User;
import com.omisys.user.domain.repository.UserRepository;
import com.omisys.user.domain.repository.UserDeviceRepository;
import com.omisys.user.exception.UserException;
import com.omisys.user.presentation.response.UserNotificationInfoResponse;
import com.omisys.user.presentation.response.UserDeviceInfoResponse;
import com.omisys.user_dto.infrastructure.UserDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import static com.omisys.user.exception.UserErrorCode.USER_NOT_FOUND;

@Service
@RequiredArgsConstructor
public class UserInternalService {

    private final UserRepository userRepository;
    private final UserDeviceRepository userDeviceRepository;

    public UserDto getUserByUsername(String username) {

        User user = userRepository
                .findByUsername(username)
                .orElseThrow(() -> new UserException(USER_NOT_FOUND));

        return toDto(user);

    }

    public UserDto getUserByEmail(String email) {

        User user = userRepository
                .findByEmail(email)
                .orElseThrow(() -> new UserException(USER_NOT_FOUND));

        return toDto(user);

    }

    public UserDto getUserByUserId(Long userId) {

        User user = userRepository
                .findById(userId)
                .orElseThrow(() -> new UserException(USER_NOT_FOUND));

        return toDto(user);

    }

    public UserNotificationInfoResponse getNotificationInfo(Long userId) {
        User user = userRepository
                .findById(userId)
                .orElseThrow(() -> new UserException(USER_NOT_FOUND));
        var devices = userDeviceRepository.findAllByUserId(userId).stream()
                .map(device -> new UserDeviceInfoResponse(
                        device.getDeviceId(), device.getPlatform().name(), device.getPushToken()))
                .toList();
        return new UserNotificationInfoResponse(user.getEmail(), devices);
    }

    private UserDto toDto(User user) {
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
