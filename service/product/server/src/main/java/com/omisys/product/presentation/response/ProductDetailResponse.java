package com.omisys.product.presentation.response;

import com.omisys.product.infrastructure.client.dto.ReviewSummaryDto;
import lombok.Getter;
import org.springframework.data.domain.Page;

@Getter
public class ProductDetailResponse {
    private final ProductResponse product;
    private final Page<ReviewSummaryDto> reviews;

    private ProductDetailResponse(ProductResponse product, Page<ReviewSummaryDto> reviews) {
        this.product = product;
        this.reviews = reviews;
    }

    public static ProductDetailResponse of(ProductResponse product, Page<ReviewSummaryDto> reviews) {
        return new ProductDetailResponse(product, reviews);
    }
}
