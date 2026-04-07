package com.omisys.product.infrastructure.messaging;

import com.omisys.product.domain.model.outbox.OutboxEvent;
import com.omisys.product.domain.model.outbox.OutboxStatus;
import com.omisys.product.domain.repository.jpa.OutboxEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.concurrent.TimeUnit;

@Component
@RequiredArgsConstructor
@Slf4j(topic = "OutboxEventPoller")
public class OutboxEventPoller {

    private final OutboxEventRepository outboxRepo;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    /**
     * 1초마다 PENDING 이벤트를 최대 100건 조회하여 Kafka에 발행한다.
     * 발행 성공 시 PUBLISHED, 실패 시 FAILED로 상태를 갱신한다.
     */
    @Scheduled(fixedDelay = 1_000)
    @Transactional
    public void publish() {
        List<OutboxEvent> pending =
                outboxRepo.findTop100ByStatusOrderByCreatedAtAsc(OutboxStatus.PENDING);

        for (OutboxEvent event : pending) {
            try {
                kafkaTemplate.send(event.getEventType(), event.getMessageKey(), event.getPayload())
                        .get(5, TimeUnit.SECONDS);
                event.markPublished();
                log.info("Outbox published: id={} topic={}", event.getId(), event.getEventType());
            } catch (Exception e) {
                event.markFailed();
                log.error("Outbox publish failed: id={} topic={}", event.getId(), event.getEventType(), e);
            }
        }
    }
}
