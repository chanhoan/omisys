package com.omisys.payment.server.infrastructure.messaging;

import com.omisys.payment.server.domain.model.outbox.OutboxEvent;
import com.omisys.payment.server.domain.model.outbox.OutboxStatus;
import com.omisys.payment.server.domain.repository.OutboxEventRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OutboxEventPollerTest {

    @Mock private OutboxEventRepository outboxRepo;
    @Mock private KafkaTemplate<String, Object> kafkaTemplate;

    private OutboxEventPoller poller;

    @BeforeEach
    void setUp() {
        poller = new OutboxEventPoller(outboxRepo, kafkaTemplate);
    }

    @Test
    @DisplayName("publish: PENDING 이벤트가 있으면 kafkaTemplate.send()를 호출한다")
    void publish_pendingEvent_callsKafkaSend() {
        // given
        OutboxEvent event = OutboxEvent.pending("Payment", "99", "payment-completed-topic",
                "10", "{\"success\":true}");
        when(outboxRepo.findTop100ByStatusOrderByCreatedAtAsc(OutboxStatus.PENDING))
                .thenReturn(List.of(event));
        when(kafkaTemplate.send(anyString(), anyString(), any()))
                .thenReturn(CompletableFuture.completedFuture(mock(SendResult.class)));

        // when
        poller.publish();

        // then
        verify(kafkaTemplate).send("payment-completed-topic", "10", "{\"success\":true}");
    }

    @Test
    @DisplayName("publish: Kafka 발행 성공 시 이벤트 상태가 PUBLISHED로 변경된다")
    void publish_kafkaSuccess_marksEventPublished() {
        // given
        OutboxEvent event = OutboxEvent.pending("Payment", "99", "payment-completed-topic",
                "10", "{\"success\":true}");
        when(outboxRepo.findTop100ByStatusOrderByCreatedAtAsc(OutboxStatus.PENDING))
                .thenReturn(List.of(event));
        when(kafkaTemplate.send(anyString(), anyString(), any()))
                .thenReturn(CompletableFuture.completedFuture(mock(SendResult.class)));

        // when
        poller.publish();

        // then
        assertThat(event.getStatus()).isEqualTo(OutboxStatus.PUBLISHED);
        assertThat(event.getPublishedAt()).isNotNull();
    }

    @Test
    @DisplayName("publish: Kafka 발행 실패 시 이벤트 상태가 FAILED로 변경된다")
    void publish_kafkaFails_marksEventFailed() {
        // given
        OutboxEvent event = OutboxEvent.pending("Payment", "99", "payment-completed-topic",
                "10", "{\"success\":true}");
        when(outboxRepo.findTop100ByStatusOrderByCreatedAtAsc(OutboxStatus.PENDING))
                .thenReturn(List.of(event));

        CompletableFuture<SendResult<String, Object>> failedFuture = new CompletableFuture<>();
        failedFuture.completeExceptionally(new RuntimeException("Kafka down"));
        when(kafkaTemplate.send(anyString(), anyString(), any())).thenReturn(failedFuture);

        // when
        poller.publish();

        // then
        assertThat(event.getStatus()).isEqualTo(OutboxStatus.FAILED);
    }

    @Test
    @DisplayName("publish: PENDING 이벤트가 없으면 kafkaTemplate을 호출하지 않는다")
    void publish_noPendingEvents_doesNotCallKafka() {
        // given
        when(outboxRepo.findTop100ByStatusOrderByCreatedAtAsc(OutboxStatus.PENDING))
                .thenReturn(List.of());

        // when
        poller.publish();

        // then
        verifyNoInteractions(kafkaTemplate);
    }
}
