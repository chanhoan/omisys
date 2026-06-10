package com.omisys.user.application.service;

import com.omisys.user.domain.model.User;
import com.omisys.user.domain.model.UserDevice;
import com.omisys.user.domain.repository.UserDeviceRepository;
import com.omisys.user.domain.repository.UserRepository;
import com.omisys.user.exception.UserException;
import com.omisys.user.presentation.request.UserDeviceRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import static com.omisys.user.exception.UserErrorCode.USER_NOT_FOUND;

@Service
@RequiredArgsConstructor
public class UserDeviceService {
    private final UserRepository userRepository;
    private final UserDeviceRepository deviceRepository;

    @Transactional
    public void register(Long userId, String deviceId, UserDeviceRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserException(USER_NOT_FOUND));

        boolean removedConflictingToken = deviceRepository.findByPushToken(request.pushToken())
                .filter(device -> !device.getDeviceId().equals(deviceId))
                .map(device -> {
                    deviceRepository.delete(device);
                    return true;
                })
                .orElse(false);
        if (removedConflictingToken) {
            deviceRepository.flush();
        }

        UserDevice device = deviceRepository.findByDeviceId(deviceId)
                .orElseGet(() -> UserDevice.create(user, deviceId, request.platform(),
                        request.pushToken(), request.appVersion()));
        device.update(user, request.platform(), request.pushToken(), request.appVersion());
        deviceRepository.save(device);
    }

    @Transactional(readOnly = true)
    public boolean isRegistered(Long userId, String deviceId) {
        return deviceRepository.findByDeviceId(deviceId)
                .map(device -> device.getUser().getId().equals(userId))
                .orElse(false);
    }

    @Transactional
    public void delete(Long userId, String deviceId) {
        deviceRepository.findByDeviceId(deviceId)
                .filter(device -> device.getUser().getId().equals(userId))
                .ifPresent(deviceRepository::delete);
    }

    @Transactional
    public void deleteInternal(String deviceId) {
        deviceRepository.deleteByDeviceId(deviceId);
    }
}
