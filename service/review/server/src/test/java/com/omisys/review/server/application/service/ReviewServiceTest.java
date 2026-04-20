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
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReviewServiceTest {

    private static final String PRODUCT_ID = "550e8400-e29b-41d4-a716-446655440000";

    @Mock private ReviewRepository reviewRepository;
    @Mock private ReviewSummaryRepository reviewSummaryRepository;
    @Mock private OrderClient orderClient;
    @Mock private ReviewRatingProducer reviewRatingProducer;

    @InjectMocks private ReviewService reviewService;

    // ───── createReview ─────

    @Test
    @DisplayName("createReview: 구매 확정 주문이 없으면 ORDER_NOT_PURCHASE_CONFIRMED 예외")
    void createReview_fail_order_not_confirmed() {
        Long userId = 1L;
        ReviewRequest.Create request = ReviewRequest.Create.builder()
                .productId(PRODUCT_ID)
                .orderId(100L)
                .rating(5)
                .content("좋아요")
                .build();

        when(orderClient.isPurchaseConfirmed(request.getOrderId(), userId)).thenReturn(false);

        assertThatThrownBy(() -> reviewService.createReview(userId, request))
                .isInstanceOf(ReviewException.class)
                .satisfies(ex -> assertThat(((ReviewException) ex).getErrorCode())
                        .isEqualTo(ReviewErrorCode.ORDER_NOT_PURCHASE_CONFIRMED));
    }

    @Test
    @DisplayName("createReview: 동일 상품에 이미 리뷰를 작성했으면 REVIEW_ALREADY_EXISTS 예외")
    void createReview_fail_duplicate_review() {
        Long userId = 1L;
        ReviewRequest.Create request = ReviewRequest.Create.builder()
                .productId(PRODUCT_ID)
                .orderId(100L)
                .rating(4)
                .content("중복 리뷰")
                .build();

        when(orderClient.isPurchaseConfirmed(request.getOrderId(), userId)).thenReturn(true);
        when(reviewRepository.existsByProductIdAndUserId(request.getProductId(), userId)).thenReturn(true);

        assertThatThrownBy(() -> reviewService.createReview(userId, request))
                .isInstanceOf(ReviewException.class)
                .satisfies(ex -> assertThat(((ReviewException) ex).getErrorCode())
                        .isEqualTo(ReviewErrorCode.REVIEW_ALREADY_EXISTS));
    }

    @Test
    @DisplayName("createReview: 정상 작성 시 Review 저장 후 Kafka 이벤트 발행")
    void createReview_success() {
        Long userId = 1L;
        ReviewRequest.Create request = ReviewRequest.Create.builder()
                .productId(PRODUCT_ID)
                .orderId(100L)
                .rating(5)
                .content("최고!")
                .build();

        Review savedReview = Review.create(userId, request);
        ReviewSummary summary = ReviewSummary.init(PRODUCT_ID);

        when(orderClient.isPurchaseConfirmed(request.getOrderId(), userId)).thenReturn(true);
        when(reviewRepository.existsByProductIdAndUserId(request.getProductId(), userId)).thenReturn(false);
        when(reviewRepository.save(any(Review.class))).thenReturn(savedReview);
        when(reviewSummaryRepository.findByProductId(request.getProductId())).thenReturn(Optional.of(summary));

        reviewService.createReview(userId, request);

        verify(reviewRepository).save(any(Review.class));
        verify(reviewRatingProducer).publish(eq(PRODUCT_ID), anyDouble(), anyLong());
    }

    // ───── updateReview ─────

    @Test
    @DisplayName("updateReview: 본인이 아니면 REVIEW_FORBIDDEN 예외")
    void updateReview_fail_not_owner() {
        Long userId = 2L;
        Long reviewId = 1L;
        ReviewRequest.Update request = ReviewRequest.Update.builder()
                .rating(3)
                .content("수정 내용")
                .build();

        Review review = Review.create(1L, ReviewRequest.Create.builder()
                .productId(PRODUCT_ID).orderId(100L).rating(5).content("원본").build());

        when(reviewRepository.findById(reviewId)).thenReturn(Optional.of(review));

        assertThatThrownBy(() -> reviewService.updateReview(userId, reviewId, request))
                .isInstanceOf(ReviewException.class)
                .satisfies(ex -> assertThat(((ReviewException) ex).getErrorCode())
                        .isEqualTo(ReviewErrorCode.REVIEW_FORBIDDEN));
    }

    @Test
    @DisplayName("updateReview: 본인이면 내용과 평점이 수정된다")
    void updateReview_success() {
        Long userId = 1L;
        Long reviewId = 1L;
        ReviewRequest.Update request = ReviewRequest.Update.builder()
                .rating(3)
                .content("수정된 내용")
                .build();

        Review review = Review.create(userId, ReviewRequest.Create.builder()
                .productId(PRODUCT_ID).orderId(100L).rating(5).content("원본").build());
        ReviewSummary summary = ReviewSummary.init(PRODUCT_ID);

        when(reviewRepository.findById(reviewId)).thenReturn(Optional.of(review));
        when(reviewSummaryRepository.findByProductId(review.getProductId())).thenReturn(Optional.of(summary));

        reviewService.updateReview(userId, reviewId, request);

        assertThat(review.getRating()).isEqualTo(3);
        assertThat(review.getContent()).isEqualTo("수정된 내용");
        verify(reviewRatingProducer).publish(eq(PRODUCT_ID), anyDouble(), anyLong());
    }

    // ───── deleteReview ─────

    @Test
    @DisplayName("deleteReview: 본인이 아니면 REVIEW_FORBIDDEN 예외")
    void deleteReview_fail_not_owner() {
        Long userId = 2L;
        Long reviewId = 1L;

        Review review = Review.create(1L, ReviewRequest.Create.builder()
                .productId(PRODUCT_ID).orderId(100L).rating(5).content("내용").build());

        when(reviewRepository.findById(reviewId)).thenReturn(Optional.of(review));

        assertThatThrownBy(() -> reviewService.deleteReview(userId, reviewId))
                .isInstanceOf(ReviewException.class)
                .satisfies(ex -> assertThat(((ReviewException) ex).getErrorCode())
                        .isEqualTo(ReviewErrorCode.REVIEW_FORBIDDEN));
    }

    @Test
    @DisplayName("deleteReview: 본인이면 삭제 후 Kafka 이벤트 발행")
    void deleteReview_success() {
        Long userId = 1L;
        Long reviewId = 1L;

        Review review = Review.create(userId, ReviewRequest.Create.builder()
                .productId(PRODUCT_ID).orderId(100L).rating(5).content("내용").build());
        ReviewSummary summary = ReviewSummary.init(PRODUCT_ID);

        when(reviewRepository.findById(reviewId)).thenReturn(Optional.of(review));
        when(reviewSummaryRepository.findByProductId(review.getProductId())).thenReturn(Optional.of(summary));

        reviewService.deleteReview(userId, reviewId);

        verify(reviewRepository).delete(review);
        verify(reviewRatingProducer).publish(eq(PRODUCT_ID), anyDouble(), anyLong());
    }

    // ───── REVIEW_NOT_FOUND ─────

    @Test
    @DisplayName("updateReview: 리뷰가 없으면 REVIEW_NOT_FOUND 예외")
    void updateReview_fail_not_found() {
        when(reviewRepository.findById(anyLong())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> reviewService.updateReview(1L, 99L,
                ReviewRequest.Update.builder().rating(3).content("x").build()))
                .isInstanceOf(ReviewException.class)
                .satisfies(ex -> assertThat(((ReviewException) ex).getErrorCode())
                        .isEqualTo(ReviewErrorCode.REVIEW_NOT_FOUND));
    }
}
