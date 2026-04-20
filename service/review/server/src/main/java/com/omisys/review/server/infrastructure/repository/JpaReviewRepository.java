package com.omisys.review.server.infrastructure.repository;

import com.omisys.review.server.domain.model.Review;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface JpaReviewRepository extends JpaRepository<Review, Long> {
    boolean existsByProductIdAndUserId(String productId, Long userId);
    Page<Review> findAllByProductId(String productId, Pageable pageable);

    @Query("SELECT COALESCE(SUM(r.rating), 0) FROM Review r WHERE r.productId = :productId")
    double sumRatingByProductId(@Param("productId") String productId);

    long countByProductId(String productId);
}
