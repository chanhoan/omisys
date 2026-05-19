package com.omisys.notification.server.infrastructure.notification;

import com.google.firebase.FirebaseApp;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.Notification;
import com.omisys.notification.server.domain.model.vo.NotificationType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class FcmNotificationService {

    public void send(String fcmToken, NotificationType type, String productName, long orderId) {
        if (fcmToken == null || fcmToken.isBlank()) {
            return;
        }
        if (FirebaseApp.getApps().isEmpty()) {
            log.warn("Firebase not initialized — skipping FCM for token={}", fcmToken.substring(0, 10));
            return;
        }
        try {
            Message message = Message.builder()
                    .setToken(fcmToken)
                    .setNotification(Notification.builder()
                            .setTitle(title(type))
                            .setBody("%s 외 주문(#%d)".formatted(productName, orderId))
                            .build())
                    .build();
            String response = FirebaseMessaging.getInstance().send(message);
            log.info("FCM sent: {} type={}", response, type);
        } catch (Exception e) {
            log.error("FCM send failed type={}", type, e);
            throw new RuntimeException(e);
        }
    }

    private String title(NotificationType type) {
        return switch (type) {
            case ORDER_COMPLETED -> "주문 완료";
            case SHIPPING_STARTED -> "배송 시작";
            case DELIVERED -> "배송 완료";
            case PURCHASE_CONFIRMED -> "구매 확정";
            case ORDER_CANCELED -> "주문 취소";
        };
    }
}
