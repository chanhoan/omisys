package com.omisys.product.application.preorder;

import com.omisys.common.domain.entity.KafkaTopicConstant;
import com.omisys.order.order_dto.dto.OrderCreateRequest;
import com.omisys.product.infrastructure.messaging.PreOrderProducer;
import com.omisys.product.infrastructure.utils.PreOrderRedisDto;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PreOrderFacadeServiceTest {

    @Mock private PreOrderProducer preOrderProducer;
    @Mock private PreOrderLockService preOrderLockService;

    @InjectMocks
    private PreOrderFacadeService preOrderFacadeService;

    @Test
    @DisplayName("preOrder: reservation → OrderCreateRequest 생성 → Kafka 발행")
    void preOrder_success() {
        // given
        long preOrderId = 10L;
        long addressId = 99L;
        long userId = 777L;

        PreOrderRedisDto cached = new PreOrderRedisDto(
                preOrderId,
                "product-uuid-string",
                100,
                LocalDateTime.now().minusDays(1),
                LocalDateTime.now().plusDays(1)
        );

        when(preOrderLockService.reservation(preOrderId, userId)).thenReturn(cached);

        // when
        preOrderFacadeService.preOrder(preOrderId, addressId, userId);

        // then
        ArgumentCaptor<OrderCreateRequest> reqCaptor = ArgumentCaptor.forClass(OrderCreateRequest.class);
        verify(preOrderProducer).send(
                eq(KafkaTopicConstant.PROCESS_PREORDER),
                eq(Long.toString(userId)),
                reqCaptor.capture()
        );

        OrderCreateRequest sent = reqCaptor.getValue();

        // OrderCreateRequest의 내부 구조는 order_dto 정의에 따르므로, 핵심만 검증한다.
        assertThat(sent.getOrderType()).isEqualTo("PREORDER");
        assertThat(sent.getOrderProductInfos()).hasSize(1);
        assertThat(sent.getOrderProductInfos().get(0).getProductId()).isEqualTo("product-uuid-string");
        assertThat(sent.getOrderProductInfos().get(0).getQuantity()).isEqualTo(1);
        assertThat(sent.getAddressId()).isEqualTo(addressId);
    }
}
