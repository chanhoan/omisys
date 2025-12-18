package com.omisys.order.server.application.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.omisys.common.domain.entity.KafkaTopicConstant;
import com.omisys.order.server.domain.model.Order;
import com.omisys.order.server.exception.OrderErrorCode;
import com.omisys.order.server.exception.OrderException;
import com.omisys.order.server.infrastructure.event.PaymentCompletedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j(topic = "OrderEventHandler")
@RequiredArgsConstructor
public class OrderEventHandler {

    private final OrderService orderService;

    @Transactional
    @KafkaListener(topics = KafkaTopicConstant.PAYMENT_COMPLETED, groupId = "order-service-group")
    public void handlePaymentCompleteEvent(String event) {
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            PaymentCompletedEvent paymentCompletedEvent = objectMapper.readValue(event,
                    PaymentCompletedEvent.class);
            Boolean success = paymentCompletedEvent.getSuccess();

            if (success) {
                log.info("===== Payment complete event success =====");
                Order order = orderService.validateOrderExists(paymentCompletedEvent.getOrderId());
                order.complete();
                order.setPaymentId(paymentCompletedEvent.getOrderId());
            } else {
                log.info("===== Payment complete event fail =====");
                orderService.cancelOrder(paymentCompletedEvent.getUserId(),
                        paymentCompletedEvent.getOrderId());
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            throw new OrderException(OrderErrorCode.EVENT_PROCESSING_FAILED);
        }
    }

}
