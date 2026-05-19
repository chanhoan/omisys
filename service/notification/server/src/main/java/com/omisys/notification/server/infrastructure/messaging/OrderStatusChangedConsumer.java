package com.omisys.notification.server.infrastructure.messaging;

import com.omisys.common.domain.entity.KafkaTopicConstant;
import com.omisys.notification.server.application.service.NotificationService;
import com.omisys.order.order_dto.dto.NotificationOrderDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class OrderStatusChangedConsumer {

    private final NotificationService notificationService;

    @KafkaListener(topics = KafkaTopicConstant.ORDER_STATUS_CHANGED,
                   groupId = "notification-service-group")
    public void consume(@Payload NotificationOrderDto dto) {
        log.info("Received order status changed event: orderId={} userId={} state={}",
                dto.getOrderId(), dto.getUserId(), dto.getOrderState());
        notificationService.processNotification(dto);
    }
}
