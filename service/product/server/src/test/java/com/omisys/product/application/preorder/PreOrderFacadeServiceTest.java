package com.omisys.product.application.preorder;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.omisys.common.domain.entity.KafkaTopicConstant;
import com.omisys.product.domain.model.outbox.OutboxEvent;
import com.omisys.product.domain.model.outbox.OutboxStatus;
import com.omisys.product.domain.repository.jpa.OutboxEventRepository;
import com.omisys.product.infrastructure.utils.PreOrderRedisDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PreOrderFacadeServiceTest {

    @Mock private PreOrderLockService preOrderLockService;
    @Mock private OutboxEventRepository outboxEventRepository;

    private PreOrderFacadeService preOrderFacadeService;

    @BeforeEach
    void setUp() {
        preOrderFacadeService = new PreOrderFacadeService(
                preOrderLockService, outboxEventRepository, new ObjectMapper());
    }

    @Test
    @DisplayName("preOrder: reservation 후 OutboxEvent가 PENDING 상태로 저장된다")
    void preOrder_savesOutboxEvent() {
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
        ArgumentCaptor<OutboxEvent> captor = ArgumentCaptor.forClass(OutboxEvent.class);
        verify(outboxEventRepository).save(captor.capture());

        OutboxEvent saved = captor.getValue();
        assertThat(saved.getStatus()).isEqualTo(OutboxStatus.PENDING);
        assertThat(saved.getEventType()).isEqualTo(KafkaTopicConstant.PROCESS_PREORDER);
        assertThat(saved.getMessageKey()).isEqualTo(Long.toString(userId));
        assertThat(saved.getAggregateType()).isEqualTo("PreOrder");
    }

    @Test
    @DisplayName("preOrder: OutboxEvent payload가 PREORDER 타입의 OrderCreateRequest JSON을 포함한다")
    void preOrder_outboxEventPayloadContainsOrderCreateRequest() throws Exception {
        // given
        long preOrderId = 10L;
        long addressId = 99L;
        long userId = 777L;

        PreOrderRedisDto cached = new PreOrderRedisDto(
                preOrderId, "product-uuid-string", 100,
                LocalDateTime.now().minusDays(1), LocalDateTime.now().plusDays(1)
        );
        when(preOrderLockService.reservation(preOrderId, userId)).thenReturn(cached);

        // when
        preOrderFacadeService.preOrder(preOrderId, addressId, userId);

        // then
        ArgumentCaptor<OutboxEvent> captor = ArgumentCaptor.forClass(OutboxEvent.class);
        verify(outboxEventRepository).save(captor.capture());

        String payload = captor.getValue().getPayload();
        assertThat(payload).contains("PREORDER");
        assertThat(payload).contains("product-uuid-string");
    }

    @Test
    @DisplayName("preOrder: Kafka를 직접 호출하지 않는다 (OutboxPoller에 위임)")
    void preOrder_doesNotCallKafkaDirectly() {
        // given
        long preOrderId = 10L;
        long addressId = 99L;
        long userId = 777L;

        PreOrderRedisDto cached = new PreOrderRedisDto(
                preOrderId, "product-uuid-string", 100,
                LocalDateTime.now().minusDays(1), LocalDateTime.now().plusDays(1)
        );
        when(preOrderLockService.reservation(preOrderId, userId)).thenReturn(cached);

        // when & then — KafkaTemplate 주입 없이도 예외 없이 동작해야 함
        preOrderFacadeService.preOrder(preOrderId, addressId, userId);
        verify(outboxEventRepository).save(any(OutboxEvent.class));
    }
}
