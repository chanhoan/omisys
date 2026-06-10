package com.omisys.user.application.service;

import com.omisys.user.domain.model.User;
import com.omisys.user.domain.model.UserDevice;
import com.omisys.user.domain.model.vo.DevicePlatform;
import com.omisys.user.domain.repository.UserDeviceRepository;
import com.omisys.user.domain.repository.UserRepository;
import com.omisys.user.presentation.request.UserDeviceRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserDeviceServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private UserDeviceRepository userDeviceRepository;
    @InjectMocks private UserDeviceService userDeviceService;

    @Test
    void registersNewDeviceForAuthenticatedUser() {
        User user = mock(User.class);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userDeviceRepository.findByDeviceId("device-1")).thenReturn(Optional.empty());
        when(userDeviceRepository.findByPushToken("token-1")).thenReturn(Optional.empty());

        userDeviceService.register(1L, "device-1",
                new UserDeviceRequest(DevicePlatform.ANDROID, "token-1", "0.1.0"));

        verify(userDeviceRepository).save(argThat(device ->
                device.getUser() == user
                        && device.getDeviceId().equals("device-1")
                        && device.getPushToken().equals("token-1")));
    }

    @Test
    void transfersExistingInstallationToCurrentUserAndRefreshesToken() {
        User currentUser = mock(User.class);
        UserDevice device = mock(UserDevice.class);
        when(userRepository.findById(2L)).thenReturn(Optional.of(currentUser));
        when(userDeviceRepository.findByDeviceId("device-1")).thenReturn(Optional.of(device));
        when(userDeviceRepository.findByPushToken("new-token")).thenReturn(Optional.empty());

        userDeviceService.register(2L, "device-1",
                new UserDeviceRequest(DevicePlatform.IOS, "new-token", "0.2.0"));

        verify(device).update(currentUser, DevicePlatform.IOS, "new-token", "0.2.0");
        verify(userDeviceRepository).save(device);
    }

    @Test
    void removesOnlyCurrentUsersDevice() {
        User owner = mock(User.class);
        when(owner.getId()).thenReturn(1L);
        UserDevice device = mock(UserDevice.class);
        when(device.getUser()).thenReturn(owner);
        when(userDeviceRepository.findByDeviceId("device-1")).thenReturn(Optional.of(device));

        userDeviceService.delete(2L, "device-1");

        verify(userDeviceRepository, never()).delete(any());
    }

    @Test
    void reportsRegistrationOnlyForOwner() {
        User owner = mock(User.class);
        when(owner.getId()).thenReturn(1L);
        UserDevice device = mock(UserDevice.class);
        when(device.getUser()).thenReturn(owner);
        when(userDeviceRepository.findByDeviceId("device-1")).thenReturn(Optional.of(device));

        assertThat(userDeviceService.isRegistered(1L, "device-1")).isTrue();
        assertThat(userDeviceService.isRegistered(2L, "device-1")).isFalse();
    }
}
