package com.omisys.notification.server.application.service;

import com.omisys.notification.server.domain.repository.NotificationRepository;
import com.omisys.notification.server.infrastructure.client.UserClient;
import com.omisys.notification.server.infrastructure.client.dto.UserDeviceInfo;
import com.omisys.notification.server.infrastructure.client.dto.UserNotificationInfo;
import com.omisys.notification.server.infrastructure.notification.EmailNotificationService;
import com.omisys.notification.server.infrastructure.notification.FcmNotificationService;
import com.omisys.notification.server.infrastructure.notification.InvalidFcmTokenException;
import com.omisys.order.order_dto.dto.NotificationOrderDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock private UserClient userClient;
    @Mock private EmailNotificationService emailService;
    @Mock private FcmNotificationService fcmService;
    @Mock private NotificationRepository notificationRepository;
    private NotificationService notificationService;

    @BeforeEach
    void setUp() {
        notificationService = new NotificationService(
                userClient, emailService, fcmService, notificationRepository);
    }

    @Test
    void sendsOrderNotificationToEveryRegisteredAppDevice() {
        NotificationOrderDto dto = orderDto();
        when(userClient.getNotificationInfo(7L)).thenReturn(new UserNotificationInfo(
                "user@example.com",
                List.of(new UserDeviceInfo("ios-1", "IOS", "token-1"),
                        new UserDeviceInfo("android-1", "ANDROID", "token-2"))));

        notificationService.processNotification(dto);

        verify(fcmService).send(eq("token-1"), any(), eq("상품"), eq(42L));
        verify(fcmService).send(eq("token-2"), any(), eq("상품"), eq(42L));
    }

    @Test
    void deletesInvalidDeviceWithoutStoppingOtherDeliveries() {
        NotificationOrderDto dto = orderDto();
        when(userClient.getNotificationInfo(7L)).thenReturn(new UserNotificationInfo(
                "user@example.com",
                List.of(new UserDeviceInfo("bad-device", "IOS", "bad-token"),
                        new UserDeviceInfo("good-device", "ANDROID", "good-token"))));
        doThrow(new InvalidFcmTokenException("bad-token"))
                .when(fcmService).send(eq("bad-token"), any(), anyString(), eq(42L));

        notificationService.processNotification(dto);

        verify(userClient).deleteDevice("bad-device");
        verify(fcmService).send(eq("good-token"), any(), eq("상품"), eq(42L));
    }

    private NotificationOrderDto orderDto() {
        NotificationOrderDto dto = mock(NotificationOrderDto.class);
        when(dto.getUserId()).thenReturn(7L);
        when(dto.getOrderId()).thenReturn(42L);
        when(dto.getOrderState()).thenReturn("SHIPPING");
        when(dto.getDisplayProductName()).thenReturn("상품");
        return dto;
    }
}
