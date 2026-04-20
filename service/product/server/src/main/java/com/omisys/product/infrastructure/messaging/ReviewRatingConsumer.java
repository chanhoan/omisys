package com.omisys.product.infrastructure.messaging;

import com.omisys.common.domain.entity.KafkaTopicConstant;
import com.omisys.product.domain.model.Product;
import com.omisys.product.domain.repository.cassandra.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class ReviewRatingConsumer {

    private final ProductRepository productRepository;

    @KafkaListener(topics = KafkaTopicConstant.REVIEW_RATING, groupId = "product-review-rating-group", concurrency = "1")
    public void consume(Map<String, Object> payload) {
        try {
            UUID productId = UUID.fromString(payload.get("productId").toString());
            double avgRating = Double.parseDouble(payload.get("avgRating").toString());
            long reviewCount = Long.parseLong(payload.get("reviewCount").toString());

            productRepository.findByProductIdAndIsDeletedFalse(productId).ifPresent(product -> {
                product.updateRating(avgRating, reviewCount);
                productRepository.save(product);
                log.info("Updated product rating: productId={}, avgRating={}, reviewCount={}", productId, avgRating, reviewCount);
            });
        } catch (Exception e) {
            log.error("Failed to process review-rating event: payload={}", payload, e);
            throw e;
        }
    }
}
