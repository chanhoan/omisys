package com.omisys.product.infrastructure.messaging;

import com.omisys.product.domain.model.outbox.OutboxEvent;
import com.omisys.product.domain.model.outbox.OutboxStatus;
import com.omisys.product.domain.repository.jpa.OutboxEventRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings("unchecked")
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
        OutboxEvent event = OutboxEvent.pending("PreOrder", "10", "process_preorder",
                "777", "{\"orderType\":\"PREORDER\"}");
        when(outboxRepo.findPendingReadyToPublish(any(LocalDateTime.class), any(Pageable.class)))
                .thenReturn(List.of(event));
        when(kafkaTemplate.send(anyString(), anyString(), any()))
                .thenReturn(CompletableFuture.completedFuture(mock(SendResult.class)));

        poller.publish();

        verify(kafkaTemplate).send("process_preorder", "777", "{\"orderType\":\"PREORDER\"}");
    }

    @Test
    @DisplayName("publish: Kafka 발행 성공 시 이벤트 상태가 PUBLISHED로 변경된다")
    void publish_kafkaSuccess_marksEventPublished() {
        OutboxEvent event = OutboxEvent.pending("PreOrder", "10", "process_preorder",
                "777", "{\"orderType\":\"PREORDER\"}");
        when(outboxRepo.findPendingReadyToPublish(any(LocalDateTime.class), any(Pageable.class)))
                .thenReturn(List.of(event));
        when(kafkaTemplate.send(anyString(), anyString(), any()))
                .thenReturn(CompletableFuture.completedFuture(mock(SendResult.class)));

        poller.publish();

        assertThat(event.getStatus()).isEqualTo(OutboxStatus.PUBLISHED);
        assertThat(event.getPublishedAt()).isNotNull();
    }

    @Test
    @DisplayName("publish: Kafka 1회 실패 시 상태가 PENDING 유지되고 retryCount가 1 증가한다")
    void publish_firstKafkaFailure_keepsPendingWithRetryCount1() {
        OutboxEvent event = OutboxEvent.pending("PreOrder", "10", "process_preorder",
                "777", "{\"orderType\":\"PREORDER\"}");
        when(outboxRepo.findPendingReadyToPublish(any(LocalDateTime.class), any(Pageable.class)))
                .thenReturn(List.of(event));
        CompletableFuture<SendResult<String, Object>> failedFuture = new CompletableFuture<>();
        failedFuture.completeExceptionally(new RuntimeException("Kafka down"));
        when(kafkaTemplate.send(anyString(), anyString(), any())).thenReturn(failedFuture);

        poller.publish();

        assertThat(event.getStatus()).isEqualTo(OutboxStatus.PENDING);
        assertThat(event.getRetryCount()).isEqualTo(1);
        assertThat(event.getNextRetryAt()).isNotNull();
    }

    @Test
    @DisplayName("publish: MAX_RETRY 초과 실패 시 이벤트 상태가 FAILED로 변경된다")
    void publish_maxRetryExceeded_marksEventFailed() {
        OutboxEvent event = OutboxEvent.pending("PreOrder", "10", "process_preorder",
                "777", "{\"orderType\":\"PREORDER\"}");
        for (int i = 0; i < OutboxEvent.MAX_RETRY - 1; i++) {
            event.markRetryOrFailed();
        }
        when(outboxRepo.findPendingReadyToPublish(any(LocalDateTime.class), any(Pageable.class)))
                .thenReturn(List.of(event));
        CompletableFuture<SendResult<String, Object>> failedFuture = new CompletableFuture<>();
        failedFuture.completeExceptionally(new RuntimeException("Kafka down"));
        when(kafkaTemplate.send(anyString(), anyString(), any())).thenReturn(failedFuture);

        poller.publish();

        assertThat(event.getStatus()).isEqualTo(OutboxStatus.FAILED);
        assertThat(event.getRetryCount()).isEqualTo(OutboxEvent.MAX_RETRY);
    }

    @Test
    @DisplayName("publish: 배치 내 일부 성공/일부 실패 시 각각 독립적으로 상태가 변경된다")
    void publish_partialBatchFailure_updatesEachEventIndependently() {
        OutboxEvent successEvent = OutboxEvent.pending("PreOrder", "1", "process_preorder",
                "100", "{}");
        OutboxEvent failEvent = OutboxEvent.pending("PreOrder", "2", "process_preorder",
                "200", "{}");
        when(outboxRepo.findPendingReadyToPublish(any(LocalDateTime.class), any(Pageable.class)))
                .thenReturn(List.of(successEvent, failEvent));

        CompletableFuture<SendResult<String, Object>> failedFuture = new CompletableFuture<>();
        failedFuture.completeExceptionally(new RuntimeException("Kafka down"));
        when(kafkaTemplate.send(anyString(), eq("100"), any()))
                .thenReturn(CompletableFuture.completedFuture(mock(SendResult.class)));
        when(kafkaTemplate.send(anyString(), eq("200"), any()))
                .thenReturn(failedFuture);

        poller.publish();

        assertThat(successEvent.getStatus()).isEqualTo(OutboxStatus.PUBLISHED);
        assertThat(failEvent.getStatus()).isEqualTo(OutboxStatus.PENDING);
        assertThat(failEvent.getRetryCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("publish: PENDING 이벤트가 없으면 kafkaTemplate을 호출하지 않는다")
    void publish_noPendingEvents_doesNotCallKafka() {
        when(outboxRepo.findPendingReadyToPublish(any(LocalDateTime.class), any(Pageable.class)))
                .thenReturn(List.of());

        poller.publish();

        verifyNoInteractions(kafkaTemplate);
    }
}
