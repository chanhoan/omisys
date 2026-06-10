package com.omisys.user.infrastructure.repository;

import com.omisys.user.domain.model.UserDevice;
import com.omisys.user.domain.repository.UserDeviceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class UserDeviceRepositoryImpl implements UserDeviceRepository {
    private final JpaUserDeviceRepository repository;

    @Override public UserDevice save(UserDevice device) { return repository.save(device); }
    @Override public Optional<UserDevice> findByDeviceId(String deviceId) { return repository.findByDeviceId(deviceId); }
    @Override public Optional<UserDevice> findByPushToken(String pushToken) { return repository.findByPushToken(pushToken); }
    @Override public List<UserDevice> findAllByUserId(Long userId) { return repository.findAllByUserId(userId); }
    @Override public void delete(UserDevice device) { repository.delete(device); }
    @Override public void deleteByDeviceId(String deviceId) { repository.deleteByDeviceId(deviceId); }
    @Override public void flush() { repository.flush(); }
}
