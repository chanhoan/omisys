package com.omisys.product.infrastructure.client.fallback;

import com.omisys.product.infrastructure.client.ReviewClient;
import com.omisys.product.infrastructure.client.dto.ReviewSummaryDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.stereotype.Component;

import java.util.Collections;

@Slf4j
@Component
public class ReviewClientFallback implements ReviewClient {

    @Override
    public Page<ReviewSummaryDto> getReviews(String productId, int page, int size) {
        log.warn("ReviewClient fallback triggered: productId={}", productId);
        return new PageImpl<>(Collections.emptyList());
    }
}
