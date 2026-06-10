package com.omisys.notification.server.application.service;

import com.omisys.notification.server.domain.model.Notification;
import com.omisys.notification.server.domain.model.vo.NotificationChannel;
import com.omisys.notification.server.domain.model.vo.NotificationStatus;
import com.omisys.notification.server.domain.model.vo.NotificationType;
import com.omisys.notification.server.domain.repository.NotificationRepository;
import com.omisys.notification.server.exception.NotificationErrorCode;
import com.omisys.notification.server.exception.NotificationException;
import com.omisys.notification.server.infrastructure.client.UserClient;
import com.omisys.notification.server.infrastructure.client.dto.UserNotificationInfo;
import com.omisys.notification.server.infrastructure.client.dto.UserDeviceInfo;
import com.omisys.notification.server.infrastructure.notification.EmailNotificationService;
import com.omisys.notification.server.infrastructure.notification.FcmNotificationService;
import com.omisys.notification.server.infrastructure.notification.InvalidFcmTokenException;
import com.omisys.order.order_dto.dto.NotificationOrderDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService {

    private final UserClient userClient;
    private final EmailNotificationService emailService;
    private final FcmNotificationService fcmService;
    private final NotificationRepository notificationRepository;

    public void processNotification(NotificationOrderDto dto) {
        UserNotificationInfo info = userClient.getNotificationInfo(dto.getUserId());
        if (info == null) {
            throw new NotificationException(NotificationErrorCode.USER_INFO_NOT_FOUND);
        }

        NotificationType type = toNotificationType(dto.getOrderState());
        String productName = dto.getDisplayProductName();
        long orderId = dto.getOrderId();

        sendEmail(info.email(), type, productName, orderId, dto.getUserId());
        if (info.devices() != null) {
            info.devices().forEach(device ->
                    sendFcm(device, type, productName, orderId, dto.getUserId()));
        }
    }

    private void sendEmail(String email, NotificationType type, String productName,
                           long orderId, Long userId) {
        try {
            emailService.send(email, type, productName, orderId);
            notificationRepository.save(
                    Notification.sent(userId, orderId, type, NotificationChannel.EMAIL));
        } catch (Exception e) {
            log.error("Email notification failed userId={} type={}", userId, type, e);
            notificationRepository.save(
                    Notification.failed(userId, orderId, type, NotificationChannel.EMAIL,
                            safeErrorMessage(e)));
        }
    }

    private void sendFcm(UserDeviceInfo device, NotificationType type, String productName,
                         long orderId, Long userId) {
        String fcmToken = device.pushToken();
        if (fcmToken == null || fcmToken.isBlank()) {
            return;
        }
        try {
            fcmService.send(fcmToken, type, productName, orderId);
            notificationRepository.save(
                    Notification.sent(userId, orderId, type, NotificationChannel.FCM,
                            device.deviceId()));
        } catch (InvalidFcmTokenException e) {
            log.warn("Removing invalid FCM device userId={} deviceId={}", userId, device.deviceId());
            userClient.deleteDevice(device.deviceId());
            notificationRepository.save(
                    Notification.failed(userId, orderId, type, NotificationChannel.FCM,
                            device.deviceId(), safeErrorMessage(e)));
        } catch (Exception e) {
            log.error("FCM notification failed userId={} type={}", userId, type, e);
            notificationRepository.save(
                    Notification.failed(userId, orderId, type, NotificationChannel.FCM,
                            device.deviceId(), safeErrorMessage(e)));
        }
    }

    private static String safeErrorMessage(Exception e) {
        String msg = e.getMessage();
        if (msg == null) return e.getClass().getSimpleName();
        return e.getClass().getSimpleName() + ": " + msg.substring(0, Math.min(msg.length(), 200));
    }

    private NotificationType toNotificationType(String orderState) {
        return switch (orderState) {
            case "주문 완료", "COMPLETED" -> NotificationType.ORDER_COMPLETED;
            case "배송 시작", "SHIPPING" -> NotificationType.SHIPPING_STARTED;
            case "배송 완료", "DELIVERED" -> NotificationType.DELIVERED;
            case "구매 확정", "PURCHASE_CONFIRMED" -> NotificationType.PURCHASE_CONFIRMED;
            case "주문 취소", "CANCELED" -> NotificationType.ORDER_CANCELED;
            default -> throw new NotificationException(NotificationErrorCode.INVALID_ORDER_STATE);
        };
    }
}
