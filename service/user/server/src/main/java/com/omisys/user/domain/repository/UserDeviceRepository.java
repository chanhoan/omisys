package com.omisys.user.domain.repository;

import com.omisys.user.domain.model.UserDevice;

import java.util.List;
import java.util.Optional;

public interface UserDeviceRepository {
    UserDevice save(UserDevice device);
    Optional<UserDevice> findByDeviceId(String deviceId);
    Optional<UserDevice> findByPushToken(String pushToken);
    List<UserDevice> findAllByUserId(Long userId);
    void delete(UserDevice device);
    void deleteByDeviceId(String deviceId);
    void flush();
}
