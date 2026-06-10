package com.omisys.notification.server.infrastructure.notification;

import com.google.firebase.FirebaseApp;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.MessagingErrorCode;
import com.google.firebase.messaging.AndroidConfig;
import com.google.firebase.messaging.AndroidNotification;
import com.google.firebase.messaging.ApnsConfig;
import com.google.firebase.messaging.Aps;
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
            log.warn("Firebase not initialized — skipping FCM notification");
            return;
        }
        try {
            Message message = Message.builder()
                    .setToken(fcmToken)
                    .putData("orderId", Long.toString(orderId))
                    .putData("type", type.name())
                    .setNotification(Notification.builder()
                            .setTitle(title(type))
                            .setBody("%s 외 주문(#%d)".formatted(productName, orderId))
                            .build())
                    .setAndroidConfig(AndroidConfig.builder()
                            .setNotification(AndroidNotification.builder()
                                    .setChannelId("transactions")
                                    .build())
                            .build())
                    .setApnsConfig(ApnsConfig.builder()
                            .setAps(Aps.builder().setSound("default").build())
                            .build())
                    .build();
            String response = FirebaseMessaging.getInstance().send(message);
            log.info("FCM sent: {} type={}", response, type);
        } catch (FirebaseMessagingException e) {
            if (isInvalidToken(e.getMessagingErrorCode())) {
                throw new InvalidFcmTokenException(fcmToken);
            }
            log.error("FCM send failed type={}", type, e);
            throw new RuntimeException(e);
        } catch (Exception e) {
            log.error("FCM send failed type={}", type, e);
            throw new RuntimeException(e);
        }
    }

    static boolean isInvalidToken(MessagingErrorCode errorCode) {
        return errorCode == MessagingErrorCode.UNREGISTERED;
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
