package com.omisys.order.server.infrastructure.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.omisys.order.order_dto.dto.OrderCreateRequest;
import com.omisys.order.server.application.service.OrderCreateService;
import com.omisys.order.server.domain.model.vo.OrderType;
import com.omisys.order.server.domain.repository.OrderRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PreOrderConsumerTest {

    private static final String PRODUCT_ID = "00000000-0000-4000-8000-00000000a001";
    private static final String PAYLOAD = "{\"orderType\":\"PREORDER\",\"orderProductInfos\":[{\"productId\":\"" + PRODUCT_ID + "\","
            + "\"quantity\":1,\"userCouponId\":null}],\"pointPrice\":0,\"addressId\":10001}";

    private final OrderCreateService orderCreateService = mock(OrderCreateService.class);
    private final OrderRepository orderRepository = mock(OrderRepository.class);
    private final PreOrderConsumer consumer =
            new PreOrderConsumer(orderCreateService, new ObjectMapper(), orderRepository);

    @Test
    @DisplayName("Outbox가 보낸 JSON 문자열 payload를 OrderCreateRequest로 파싱해 주문을 생성한다")
    void consume_jsonStringPayload_createsOrder() throws Exception {
        consumer.consume(PAYLOAD, "10001");

        ArgumentCaptor<OrderCreateRequest> captor = ArgumentCaptor.forClass(OrderCreateRequest.class);
        verify(orderCreateService).createOrder(eq(10001L), captor.capture());
        assertThat(captor.getValue().getOrderType()).isEqualTo("PREORDER");
        assertThat(captor.getValue().getAddressId()).isEqualTo(10001L);
        assertThat(captor.getValue().getOrderProductInfos()).hasSize(1);
    }

    @Test
    @DisplayName("같은 사용자·상품의 사전예약 주문이 이미 있으면 중복 전달로 보고 주문을 만들지 않는다")
    void consume_duplicateDelivery_skipsOrderCreation() throws Exception {
        when(orderRepository.existsByUserIdAndTypeAndOrderProducts_ProductId(10001L, OrderType.PREORDER, PRODUCT_ID))
                .thenReturn(true);

        consumer.consume(PAYLOAD, "10001");

        verify(orderCreateService, never()).createOrder(anyLong(), any());
    }
}
