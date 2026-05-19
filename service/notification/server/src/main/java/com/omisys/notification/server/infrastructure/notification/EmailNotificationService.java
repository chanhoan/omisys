package com.omisys.notification.server.infrastructure.notification;

import com.omisys.notification.server.domain.model.vo.NotificationType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailNotificationService {

    private final JavaMailSender mailSender;

    public void send(String toEmail, NotificationType type, String productName, long orderId) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(toEmail);
            message.setSubject(subject(type));
            message.setText(body(type, productName, orderId));
            mailSender.send(message);
            log.info("Email sent to={} type={}", toEmail, type);
        } catch (Exception e) {
            log.error("Email send failed to={} type={}", toEmail, type, e);
            throw e;
        }
    }

    private String subject(NotificationType type) {
        return switch (type) {
            case ORDER_COMPLETED -> "[omisys] 주문이 완료되었습니다";
            case SHIPPING_STARTED -> "[omisys] 상품이 발송되었습니다";
            case DELIVERED -> "[omisys] 상품이 배송 완료되었습니다";
            case PURCHASE_CONFIRMED -> "[omisys] 구매가 확정되었습니다";
            case ORDER_CANCELED -> "[omisys] 주문이 취소되었습니다";
        };
    }

    private String body(NotificationType type, String productName, long orderId) {
        return switch (type) {
            case ORDER_COMPLETED -> "%s 외 주문(#%d)이 완료되었습니다.".formatted(productName, orderId);
            case SHIPPING_STARTED -> "%s 외 주문(#%d)이 발송되었습니다.".formatted(productName, orderId);
            case DELIVERED -> "%s 외 주문(#%d)이 배송 완료되었습니다.".formatted(productName, orderId);
            case PURCHASE_CONFIRMED -> "%s 외 주문(#%d)의 구매가 확정되었습니다.".formatted(productName, orderId);
            case ORDER_CANCELED -> "%s 외 주문(#%d)이 취소되었습니다.".formatted(productName, orderId);
        };
    }
}
