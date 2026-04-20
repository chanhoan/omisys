package com.omisys.review.server.presentation.controller;

import com.omisys.review.server.application.service.ReviewService;
import com.omisys.review.server.presentation.response.ReviewResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/internal/reviews")
@RequiredArgsConstructor
public class ReviewInternalController {

    private final ReviewService reviewService;

    @GetMapping
    public Page<ReviewResponse.Summary> getReviews(
            @RequestParam String productId,
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return reviewService.getReviews(productId, pageable);
    }
}
