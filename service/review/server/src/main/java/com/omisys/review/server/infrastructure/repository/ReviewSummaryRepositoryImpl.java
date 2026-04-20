package com.omisys.review.server.infrastructure.repository;

import com.omisys.review.server.domain.model.ReviewSummary;
import com.omisys.review.server.domain.repository.ReviewSummaryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class ReviewSummaryRepositoryImpl implements ReviewSummaryRepository {

    private final JpaReviewSummaryRepository jpa;

    @Override
    public ReviewSummary save(ReviewSummary reviewSummary) {
        return jpa.save(reviewSummary);
    }

    @Override
    public Optional<ReviewSummary> findByProductId(String productId) {
        return jpa.findByProductId(productId);
    }
}
