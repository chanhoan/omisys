package com.omisys.delivery.server.infrastructure.messaging;

import com.omisys.delivery.server.domain.model.outbox.OutboxEvent;
import com.omisys.delivery.server.domain.model.outbox.OutboxStatus;
import com.omisys.delivery.server.infrastructure.repository.OutboxEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Component
@RequiredArgsConstructor
@Slf4j(topic = "DeliveryOutboxPoller")
public class OutboxEventPoller {

    private static final int BATCH_SIZE = 100;

    private final OutboxEventRepository outboxRepo;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Scheduled(fixedDelay = 1_000)
    public void publish() {
        List<OutboxEvent> pending = outboxRepo.findPendingReadyToPublish(
                OutboxStatus.PENDING, LocalDateTime.now(), PageRequest.of(0, BATCH_SIZE));

        for (OutboxEvent event : pending) {
            try {
                kafkaTemplate.send(event.getEventType(), event.getMessageKey(), event.getPayload())
                        .get(5, TimeUnit.SECONDS);
                event.markPublished();
                outboxRepo.save(event);
                log.info("Outbox published: id={} topic={}", event.getId(), event.getEventType());
            } catch (Exception e) {
                event.markRetryOrFailed();
                outboxRepo.save(event);
                if (event.getStatus() == OutboxStatus.FAILED) {
                    log.error("Outbox max retry exceeded, FAILED permanently: id={} retryCount={}",
                            event.getId(), event.getRetryCount(), e);
                } else {
                    log.warn("Outbox publish failed, will retry at {}: id={} retryCount={}",
                            event.getNextRetryAt(), event.getId(), event.getRetryCount());
                }
            }
        }
    }
}
