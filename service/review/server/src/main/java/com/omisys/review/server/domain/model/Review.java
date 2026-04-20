package com.omisys.review.server.domain.model;

import com.omisys.common.domain.entity.BaseEntity;
import com.omisys.review.server.exception.ReviewErrorCode;
import com.omisys.review.server.exception.ReviewException;
import com.omisys.review.server.presentation.request.ReviewRequest;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder(access = AccessLevel.PRIVATE)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Entity
@Table(name = "p_review",
        uniqueConstraints = @UniqueConstraint(columnNames = {"product_id", "user_id"}, name = "UK_REVIEW_PRODUCT_USER"))
public class Review extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "review_id")
    private Long id;

    @Column(nullable = false, length = 36)
    private String productId;

    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false)
    private Long orderId;

    @Column(nullable = false)
    private Integer rating;

    @Column(nullable = false, length = 1000)
    private String content;

    public static Review create(Long userId, ReviewRequest.Create request) {
        return Review.builder()
                .productId(request.getProductId())
                .userId(userId)
                .orderId(request.getOrderId())
                .rating(request.getRating())
                .content(request.getContent())
                .build();
    }

    public void validateOwner(Long userId) {
        if (!this.userId.equals(userId)) {
            throw new ReviewException(ReviewErrorCode.REVIEW_FORBIDDEN);
        }
    }

    public void update(ReviewRequest.Update request) {
        this.rating = request.getRating();
        this.content = request.getContent();
    }
}
