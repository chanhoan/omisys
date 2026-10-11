package com.omisys.order.server.infrastructure.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.omisys.order.order_dto.dto.OrderCreateRequest;
import com.omisys.order.server.application.service.OrderCreateService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class PreOrderConsumerTest {

    private final OrderCreateService orderCreateService = mock(OrderCreateService.class);
    private final PreOrderConsumer consumer = new PreOrderConsumer(orderCreateService, new ObjectMapper());

    @Test
    @DisplayName("Outbox가 보낸 JSON 문자열 payload를 OrderCreateRequest로 파싱해 주문을 생성한다")
    void consume_jsonStringPayload_createsOrder() throws Exception {
        String payload = "{\"orderType\":\"PREORDER\",\"orderProductInfos\":[{\"productId\":\"00000000-0000-4000-8000-00000000a001\","
                + "\"quantity\":1,\"userCouponId\":null}],\"pointPrice\":0,\"addressId\":10001}";

        consumer.consume(payload, "10001");

        ArgumentCaptor<OrderCreateRequest> captor = ArgumentCaptor.forClass(OrderCreateRequest.class);
        verify(orderCreateService).createOrder(eq(10001L), captor.capture());
        assertThat(captor.getValue().getOrderType()).isEqualTo("PREORDER");
        assertThat(captor.getValue().getAddressId()).isEqualTo(10001L);
        assertThat(captor.getValue().getOrderProductInfos()).hasSize(1);
    }
}
