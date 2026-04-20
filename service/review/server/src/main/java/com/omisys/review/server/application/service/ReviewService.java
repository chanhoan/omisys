package com.omisys.review.server.application.service;

import com.omisys.review.server.domain.model.Review;
import com.omisys.review.server.domain.model.ReviewSummary;
import com.omisys.review.server.domain.repository.ReviewRepository;
import com.omisys.review.server.domain.repository.ReviewSummaryRepository;
import com.omisys.review.server.exception.ReviewErrorCode;
import com.omisys.review.server.exception.ReviewException;
import com.omisys.review.server.infrastructure.client.OrderClient;
import com.omisys.review.server.infrastructure.messaging.ReviewRatingProducer;
import com.omisys.review.server.presentation.request.ReviewRequest;
import com.omisys.review.server.presentation.response.ReviewResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReviewService {

    private final ReviewRepository reviewRepository;
    private final ReviewSummaryRepository reviewSummaryRepository;
    private final OrderClient orderClient;
    private final ReviewRatingProducer reviewRatingProducer;

    @Transactional
    public Long createReview(Long userId, ReviewRequest.Create request) {
        if (!orderClient.isPurchaseConfirmed(request.getOrderId(), userId)) {
            throw new ReviewException(ReviewErrorCode.ORDER_NOT_PURCHASE_CONFIRMED);
        }

        if (reviewRepository.existsByProductIdAndUserId(request.getProductId(), userId)) {
            throw new ReviewException(ReviewErrorCode.REVIEW_ALREADY_EXISTS);
        }

        Review review = reviewRepository.save(Review.create(userId, request));
        publishRatingEvent(request.getProductId());
        return review.getId();
    }

    @Transactional
    public void updateReview(Long userId, Long reviewId, ReviewRequest.Update request) {
        Review review = findReviewOrThrow(reviewId);
        review.validateOwner(userId);
        review.update(request);
        publishRatingEvent(review.getProductId());
    }

    @Transactional
    public void deleteReview(Long userId, Long reviewId) {
        Review review = findReviewOrThrow(reviewId);
        review.validateOwner(userId);
        String productId = review.getProductId();
        reviewRepository.delete(review);
        publishRatingEvent(productId);
    }

    public Page<ReviewResponse.Summary> getReviews(String productId, Pageable pageable) {
        return reviewRepository.findAllByProductId(productId, pageable)
                .map(ReviewResponse.Summary::from);
    }

    private Review findReviewOrThrow(Long reviewId) {
        return reviewRepository.findById(reviewId)
                .orElseThrow(() -> new ReviewException(ReviewErrorCode.REVIEW_NOT_FOUND));
    }

    private void publishRatingEvent(String productId) {
        long count = reviewRepository.countByProductId(productId);
        double total = reviewRepository.sumRatingByProductId(productId);
        double avg = count == 0 ? 0.0 : Math.round((total / count) * 10.0) / 10.0;

        ReviewSummary summary = reviewSummaryRepository.findByProductId(productId)
                .orElse(ReviewSummary.init(productId));
        summary.recalculate(total, count);
        reviewSummaryRepository.save(summary);

        reviewRatingProducer.publish(productId, avg, count);
    }
}
