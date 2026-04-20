package com.omisys.product.infrastructure.client.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor
public class ReviewSummaryDto {
    private Long reviewId;
    private Long userId;
    private Integer rating;
    private String content;
    private LocalDateTime createdAt;
}
