package com.omisys.review.server.domain.repository;

import com.omisys.review.server.domain.model.Review;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;

public interface ReviewRepository {
    Review save(Review review);
    Optional<Review> findById(Long id);
    boolean existsByProductIdAndUserId(String productId, Long userId);
    Page<Review> findAllByProductId(String productId, Pageable pageable);
    double sumRatingByProductId(String productId);
    long countByProductId(String productId);
    void delete(Review review);
}
