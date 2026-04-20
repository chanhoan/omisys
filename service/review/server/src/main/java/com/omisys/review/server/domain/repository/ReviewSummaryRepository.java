package com.omisys.review.server.domain.repository;

import com.omisys.review.server.domain.model.ReviewSummary;

import java.util.Optional;

public interface ReviewSummaryRepository {
    ReviewSummary save(ReviewSummary reviewSummary);
    Optional<ReviewSummary> findByProductId(String productId);
}
