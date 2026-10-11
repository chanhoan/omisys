package com.omisys.notification.server.infrastructure.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.omisys.notification.server.application.service.NotificationService;
import com.omisys.order.order_dto.dto.NotificationOrderDto;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class OrderStatusChangedConsumerTest {

    private final NotificationService notificationService = mock(NotificationService.class);
    private final OrderStatusChangedConsumer consumer =
            new OrderStatusChangedConsumer(notificationService, new ObjectMapper());

    @Test
    @DisplayName("Outbox가 보낸 JSON 문자열 payload를 NotificationOrderDto로 파싱해 알림을 처리한다")
    void consume_jsonStringPayload_processesNotification() throws Exception {
        String payload = "{\"orderId\":7,\"userId\":10001,\"orderState\":\"주문 완료\","
                + "\"displayProductName\":\"상품\",\"totalQuantity\":1}";

        consumer.consume(payload);

        ArgumentCaptor<NotificationOrderDto> captor = ArgumentCaptor.forClass(NotificationOrderDto.class);
        verify(notificationService).processNotification(captor.capture());
        assertThat(captor.getValue().getOrderId()).isEqualTo(7L);
        assertThat(captor.getValue().getUserId()).isEqualTo(10001L);
        assertThat(captor.getValue().getOrderState()).isEqualTo("주문 완료");
    }
}
