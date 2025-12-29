package com.omisys.order.server.application.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.omisys.order.server.domain.model.Order;
import com.omisys.order.server.exception.OrderException;
import com.omisys.order.server.infrastructure.event.PaymentCompletedEvent;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderEventHandlerTest {

    @Mock private OrderService orderService;

    @InjectMocks private OrderEventHandler orderEventHandler;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    @DisplayName("handlePaymentCompleteEvent: success=true면 주문 상태를 COMPLETE로 전이한다")
    void handlePaymentCompleteEvent_success() throws Exception {
        // given
        PaymentCompletedEvent event = new PaymentCompletedEvent();
        event.setOrderId(10L);
        event.setUserId(1L);
        event.setSuccess(true);

        String json = objectMapper.writeValueAsString(event);

        Order order = mock(Order.class);
        when(orderService.validateOrderExists(10L)).thenReturn(order);

        // when
        orderEventHandler.handlePaymentCompleteEvent(json);

        // then
        verify(order).complete();
        // 성공 케이스에서는 cancelOrder가 호출되면 안 된다.
        verify(orderService, never()).cancelOrder(anyLong(), anyLong());
    }

    @Test
    @DisplayName("handlePaymentCompleteEvent: success=false면 cancelOrder로 보상 흐름을 탄다")
    void handlePaymentCompleteEvent_fail() throws Exception {
        // given
        PaymentCompletedEvent event = new PaymentCompletedEvent();
        event.setOrderId(10L);
        event.setUserId(1L);
        event.setSuccess(false);

        String json = objectMapper.writeValueAsString(event);

        // when
        orderEventHandler.handlePaymentCompleteEvent(json);

        // then
        verify(orderService).cancelOrder(1L, 10L);
        verify(orderService, never()).validateOrderExists(anyLong());
    }

    @Test
    @DisplayName("handlePaymentCompleteEvent: JSON 파싱 실패 시 EVENT_PROCESSING_FAILED 예외로 감싼다")
    void handlePaymentCompleteEvent_invalid_json() {
        // given
        String invalidJson = "{not-valid-json";

        // when & then
        assertThatThrownBy(() -> orderEventHandler.handlePaymentCompleteEvent(invalidJson))
                .isInstanceOf(OrderException.class);
    }
}
