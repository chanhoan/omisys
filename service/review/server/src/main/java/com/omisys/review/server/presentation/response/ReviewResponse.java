package com.omisys.review.server.presentation.response;

import com.omisys.review.server.domain.model.Review;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

public class ReviewResponse {

    @Getter
    @Builder
    public static class Summary {
        private Long reviewId;
        private Long userId;
        private Integer rating;
        private String content;
        private LocalDateTime createdAt;

        public static Summary from(Review review) {
            return Summary.builder()
                    .reviewId(review.getId())
                    .userId(review.getUserId())
                    .rating(review.getRating())
                    .content(review.getContent())
                    .createdAt(review.getCreatedAt())
                    .build();
        }
    }
}
