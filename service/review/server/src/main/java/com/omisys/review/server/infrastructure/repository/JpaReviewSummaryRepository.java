package com.omisys.review.server.infrastructure.repository;

import com.omisys.review.server.domain.model.ReviewSummary;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface JpaReviewSummaryRepository extends JpaRepository<ReviewSummary, Long> {
    Optional<ReviewSummary> findByProductId(String productId);
}
