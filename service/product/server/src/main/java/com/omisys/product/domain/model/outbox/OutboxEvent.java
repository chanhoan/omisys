package com.omisys.product.domain.model.outbox;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(
    name = "outbox_events",
    indexes = {
        @Index(name = "idx_outbox_status_retry", columnList = "status, next_retry_at, created_at")
    }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class OutboxEvent {

    public static final int MAX_RETRY = 5;
    private static final long[] BACKOFF_SECONDS = {10, 30, 60, 120, 300};

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 50)
    private String aggregateType;

    @Column(nullable = false, length = 100)
    private String aggregateId;

    @Column(nullable = false, length = 100)
    private String eventType;

    @Column(nullable = false, length = 100)
    private String messageKey;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String payload;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private OutboxStatus status;

    @Column(nullable = false)
    private int retryCount = 0;

    @Column
    private LocalDateTime nextRetryAt;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column
    private LocalDateTime publishedAt;

    public static OutboxEvent pending(String aggregateType, String aggregateId,
                                      String eventType, String messageKey, String payload) {
        OutboxEvent event = new OutboxEvent();
        event.aggregateType = aggregateType;
        event.aggregateId = aggregateId;
        event.eventType = eventType;
        event.messageKey = messageKey;
        event.payload = payload;
        event.status = OutboxStatus.PENDING;
        event.retryCount = 0;
        return event;
    }

    public void markPublished() {
        this.status = OutboxStatus.PUBLISHED;
        this.publishedAt = LocalDateTime.now();
    }

    public void markRetryOrFailed() {
        this.retryCount++;
        if (this.retryCount >= MAX_RETRY) {
            this.status = OutboxStatus.FAILED;
            this.nextRetryAt = null;
        } else {
            long backoff = BACKOFF_SECONDS[Math.min(retryCount - 1, BACKOFF_SECONDS.length - 1)];
            this.nextRetryAt = LocalDateTime.now().plusSeconds(backoff);
        }
    }
}
