package com.omisys.review.server.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Getter
@Builder(access = AccessLevel.PRIVATE)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Entity
@Table(name = "p_review_summary")
public class ReviewSummary {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 36)
    private String productId;

    @Column(nullable = false, precision = 3, scale = 1)
    private BigDecimal avgRating;

    @Column(nullable = false)
    private Long reviewCount;

    public static ReviewSummary init(String productId) {
        return ReviewSummary.builder()
                .productId(productId)
                .avgRating(BigDecimal.ZERO)
                .reviewCount(0L)
                .build();
    }

    public void recalculate(BigDecimal avgRating, long count) {
        this.reviewCount = count;
        this.avgRating = avgRating;
    }
}
