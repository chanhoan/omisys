package com.omisys.review.server.application.service;

import com.omisys.review.server.application.event.ReviewRatingChangedEvent;
import com.omisys.review.server.domain.model.Review;
import com.omisys.review.server.domain.repository.ReviewRepository;
import com.omisys.review.server.exception.ReviewErrorCode;
import com.omisys.review.server.exception.ReviewException;
import com.omisys.review.server.infrastructure.client.OrderClient;
import com.omisys.review.server.presentation.request.ReviewRequest;
import com.omisys.review.server.presentation.response.ReviewResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReviewService {

    private final ReviewRepository reviewRepository;
    private final OrderClient orderClient;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public Long createReview(Long userId, ReviewRequest.Create request) {
        if (!orderClient.isPurchaseConfirmed(request.getOrderId(), userId)) {
            throw new ReviewException(ReviewErrorCode.ORDER_NOT_PURCHASE_CONFIRMED);
        }

        if (reviewRepository.existsByProductIdAndUserId(request.getProductId(), userId)) {
            throw new ReviewException(ReviewErrorCode.REVIEW_ALREADY_EXISTS);
        }

        Review review = reviewRepository.save(Review.create(userId, request));
        eventPublisher.publishEvent(new ReviewRatingChangedEvent(request.getProductId()));
        return review.getId();
    }

    @Transactional
    public void updateReview(Long userId, Long reviewId, ReviewRequest.Update request) {
        Review review = findReviewOrThrow(reviewId);
        review.validateOwner(userId);
        review.update(request);
        eventPublisher.publishEvent(new ReviewRatingChangedEvent(review.getProductId()));
    }

    @Transactional
    public void deleteReview(Long userId, Long reviewId) {
        Review review = findReviewOrThrow(reviewId);
        review.validateOwner(userId);
        String productId = review.getProductId();
        reviewRepository.delete(review);
        eventPublisher.publishEvent(new ReviewRatingChangedEvent(productId));
    }

    public Page<ReviewResponse.Summary> getReviews(String productId, Pageable pageable) {
        return reviewRepository.findAllByProductId(productId, pageable)
                .map(ReviewResponse.Summary::from);
    }

    private Review findReviewOrThrow(Long reviewId) {
        return reviewRepository.findById(reviewId)
                .orElseThrow(() -> new ReviewException(ReviewErrorCode.REVIEW_NOT_FOUND));
    }
}
