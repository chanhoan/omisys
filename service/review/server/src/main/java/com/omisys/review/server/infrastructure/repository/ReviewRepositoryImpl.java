package com.omisys.review.server.infrastructure.repository;

import com.omisys.review.server.domain.model.Review;
import com.omisys.review.server.domain.repository.ReviewRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class ReviewRepositoryImpl implements ReviewRepository {

    private final JpaReviewRepository jpa;

    @Override
    public Review save(Review review) {
        return jpa.save(review);
    }

    @Override
    public Optional<Review> findById(Long id) {
        return jpa.findById(id);
    }

    @Override
    public boolean existsByProductIdAndUserId(String productId, Long userId) {
        return jpa.existsByProductIdAndUserId(productId, userId);
    }

    @Override
    public Page<Review> findAllByProductId(String productId, Pageable pageable) {
        return jpa.findAllByProductId(productId, pageable);
    }

    @Override
    public double sumRatingByProductId(String productId) {
        return jpa.sumRatingByProductId(productId);
    }

    @Override
    public long countByProductId(String productId) {
        return jpa.countByProductId(productId);
    }

    @Override
    public void delete(Review review) {
        jpa.delete(review);
    }
}
