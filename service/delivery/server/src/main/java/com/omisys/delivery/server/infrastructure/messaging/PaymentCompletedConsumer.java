package com.omisys.delivery.server.infrastructure.messaging;

import com.omisys.common.domain.entity.KafkaTopicConstant;
import com.omisys.delivery.server.application.service.DeliveryCreateService;
import com.omisys.delivery.server.infrastructure.event.PaymentCompletedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j(topic = "PaymentCompletedConsumer")
public class PaymentCompletedConsumer {

    private final DeliveryCreateService deliveryCreateService;

    @KafkaListener(topics = KafkaTopicConstant.PAYMENT_COMPLETED, groupId = "delivery-service-group")
    public void consume(@Payload PaymentCompletedEvent event) {
        log.info("Received payment completed event: orderId={} userId={} success={}",
                event.getOrderId(), event.getUserId(), event.getSuccess());
        deliveryCreateService.createDelivery(event);
    }
}
