package com.omisys.user.infrastructure.repository;

import com.omisys.user.domain.model.UserDevice;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface JpaUserDeviceRepository extends JpaRepository<UserDevice, Long> {
    Optional<UserDevice> findByDeviceId(String deviceId);
    Optional<UserDevice> findByPushToken(String pushToken);
    List<UserDevice> findAllByUserId(Long userId);
    void deleteByDeviceId(String deviceId);
}
