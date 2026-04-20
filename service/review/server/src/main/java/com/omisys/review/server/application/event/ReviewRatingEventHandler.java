package com.omisys.review.server.application.event;

import com.omisys.review.server.application.dto.RatingSummary;
import com.omisys.review.server.domain.model.ReviewSummary;
import com.omisys.review.server.domain.repository.ReviewRepository;
import com.omisys.review.server.domain.repository.ReviewSummaryRepository;
import com.omisys.review.server.infrastructure.messaging.ReviewRatingProducer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Slf4j
@Component
@RequiredArgsConstructor
public class ReviewRatingEventHandler {

    private final ReviewRepository reviewRepository;
    private final ReviewSummaryRepository reviewSummaryRepository;
    private final ReviewRatingProducer reviewRatingProducer;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void handle(ReviewRatingChangedEvent event) {
        String productId = event.productId();

        RatingSummary stats = reviewRepository.findRatingSummary(productId);
        long count = stats.count();
        BigDecimal avg = count == 0
                ? BigDecimal.ZERO
                : BigDecimal.valueOf(stats.totalRating()).divide(BigDecimal.valueOf(count), 1, RoundingMode.HALF_UP);

        ReviewSummary summary = reviewSummaryRepository.findByProductId(productId)
                .orElse(ReviewSummary.init(productId));
        summary.recalculate(avg, count);
        reviewSummaryRepository.save(summary);

        reviewRatingProducer.publish(productId, avg.doubleValue(), count);
        log.info("Published rating event: productId={}, avg={}, count={}", productId, avg, count);
    }
}
