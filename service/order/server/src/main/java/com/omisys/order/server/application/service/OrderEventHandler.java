package com.omisys.order.server.application.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.omisys.common.domain.entity.KafkaTopicConstant;
import com.omisys.order.order_dto.dto.NotificationOrderDto;
import com.omisys.order.server.domain.model.Order;
import com.omisys.order.server.domain.model.outbox.OutboxEvent;
import com.omisys.order.server.domain.model.vo.OrderState;
import com.omisys.order.server.exception.OrderErrorCode;
import com.omisys.order.server.exception.OrderException;
import com.omisys.order.server.infrastructure.event.PaymentCompletedEvent;
import com.omisys.order.server.infrastructure.repository.OutboxEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j(topic = "OrderEventHandler")
@RequiredArgsConstructor
public class OrderEventHandler {

    private final ObjectMapper objectMapper;
    private final OrderService orderService;
    private final OutboxEventRepository outboxEventRepository;

    @Transactional
    @KafkaListener(topics = KafkaTopicConstant.PAYMENT_COMPLETED, groupId = "order-service-group")
    public void handlePaymentCompleteEvent(String event) {
        try {
            PaymentCompletedEvent paymentCompletedEvent = objectMapper.readValue(event,
                    PaymentCompletedEvent.class);
            Boolean success = paymentCompletedEvent.getSuccess();

            if (success) {
                log.info("===== Payment complete event success =====");
                Order order = orderService.validateOrderExists(paymentCompletedEvent.getOrderId());
                order.complete();
                order.setPaymentId(paymentCompletedEvent.getOrderId());
                saveCompletedOutboxEvent(order, paymentCompletedEvent.getUserId());
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

    private void saveCompletedOutboxEvent(Order order, Long userId) {
        String displayProductName = order.getOrderProducts().get(0).getProductName();
        NotificationOrderDto dto = new NotificationOrderDto(
                order.getOrderId(), userId,
                OrderState.COMPLETED.getDescription(), displayProductName, order.getTotalQuantity());
        String payload;
        try {
            payload = objectMapper.writeValueAsString(dto);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize outbox payload for orderId=" + order.getOrderId(), e);
        }
        outboxEventRepository.save(OutboxEvent.pending(
                "ORDER", String.valueOf(order.getOrderId()),
                KafkaTopicConstant.ORDER_STATUS_CHANGED,
                String.valueOf(userId),
                payload));
    }

}
