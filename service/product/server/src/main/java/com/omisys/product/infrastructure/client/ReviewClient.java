package com.omisys.product.infrastructure.client;

import com.omisys.product.infrastructure.client.dto.ReviewSummaryDto;
import com.omisys.product.infrastructure.client.fallback.ReviewClientFallback;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "review-service", fallback = ReviewClientFallback.class)
public interface ReviewClient {

    @GetMapping("/internal/reviews")
    Page<ReviewSummaryDto> getReviews(@RequestParam String productId,
                                      @RequestParam int page,
                                      @RequestParam int size);
}
