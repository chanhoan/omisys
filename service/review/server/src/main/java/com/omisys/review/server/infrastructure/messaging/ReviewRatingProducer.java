package com.omisys.review.server.infrastructure.messaging;

import com.omisys.common.domain.entity.KafkaTopicConstant;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class ReviewRatingProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public void publish(String productId, double avgRating, long reviewCount) {
        Map<String, Object> payload = Map.of(
                "productId", productId,
                "avgRating", avgRating,
                "reviewCount", reviewCount
        );
        kafkaTemplate.send(KafkaTopicConstant.REVIEW_RATING, productId, payload);
        log.info("Published review-rating event: productId={}, avgRating={}, reviewCount={}", productId, avgRating, reviewCount);
    }
}
