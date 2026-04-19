package com.omisys.product.domain.model.outbox;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class OutboxEventTest {

    private OutboxEvent pendingEvent() {
        return OutboxEvent.pending("PreOrder", "1", "process_preorder", "777", "{}");
    }

    @Test
    @DisplayName("markRetryOrFailed: 첫 번째 실패 시 retryCount가 1 증가하고 상태가 PENDING 유지된다")
    void markRetryOrFailed_firstFailure_keepsPendingAndIncrementsRetryCount() {
        OutboxEvent event = pendingEvent();

        event.markRetryOrFailed();

        assertThat(event.getStatus()).isEqualTo(OutboxStatus.PENDING);
        assertThat(event.getRetryCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("markRetryOrFailed: 첫 번째 실패 시 nextRetryAt이 현재 시각 이후로 설정된다")
    void markRetryOrFailed_firstFailure_setsNextRetryAtInFuture() {
        OutboxEvent event = pendingEvent();
        LocalDateTime before = LocalDateTime.now();

        event.markRetryOrFailed();

        assertThat(event.getNextRetryAt()).isAfter(before);
    }

    @Test
    @DisplayName("markRetryOrFailed: MAX_RETRY 미만 실패 시 PENDING 상태를 유지한다")
    void markRetryOrFailed_belowMaxRetry_keepsPendingStatus() {
        OutboxEvent event = pendingEvent();

        for (int i = 0; i < OutboxEvent.MAX_RETRY - 1; i++) {
            event.markRetryOrFailed();
        }

        assertThat(event.getStatus()).isEqualTo(OutboxStatus.PENDING);
    }

    @Test
    @DisplayName("markRetryOrFailed: MAX_RETRY 횟수 도달 시 상태가 FAILED로 변경된다")
    void markRetryOrFailed_maxRetryReached_marksStatusFailed() {
        OutboxEvent event = pendingEvent();

        for (int i = 0; i < OutboxEvent.MAX_RETRY; i++) {
            event.markRetryOrFailed();
        }

        assertThat(event.getStatus()).isEqualTo(OutboxStatus.FAILED);
        assertThat(event.getRetryCount()).isEqualTo(OutboxEvent.MAX_RETRY);
    }

    @Test
    @DisplayName("markRetryOrFailed: MAX_RETRY 도달 시 nextRetryAt이 null로 초기화된다")
    void markRetryOrFailed_maxRetryReached_clearsNextRetryAt() {
        OutboxEvent event = pendingEvent();
        for (int i = 0; i < OutboxEvent.MAX_RETRY; i++) {
            event.markRetryOrFailed();
        }

        assertThat(event.getStatus()).isEqualTo(OutboxStatus.FAILED);
        assertThat(event.getNextRetryAt()).isNull();
    }

    @Test
    @DisplayName("markRetryOrFailed: 재시도 횟수가 증가할수록 같은 이벤트의 nextRetryAt이 더 멀어진다")
    void markRetryOrFailed_repeatedFailures_backoffIncreases() {
        OutboxEvent event = pendingEvent();

        event.markRetryOrFailed();
        LocalDateTime firstRetryAt = event.getNextRetryAt();

        event.markRetryOrFailed();
        LocalDateTime secondRetryAt = event.getNextRetryAt();

        assertThat(secondRetryAt).isAfter(firstRetryAt);
    }
}
