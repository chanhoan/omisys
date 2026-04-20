package com.omisys.review.server.presentation.controller;

import com.omisys.auth.server.auth_dto.jwt.JwtClaim;
import com.omisys.common.domain.response.ApiResponse;
import com.omisys.review.server.application.service.ReviewService;
import com.omisys.review.server.presentation.request.ReviewRequest;
import com.omisys.review.server.presentation.response.ReviewResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/reviews")
@RequiredArgsConstructor
public class ReviewController {

    private final ReviewService reviewService;

    @PostMapping
    public ResponseEntity<ApiResponse<Long>> createReview(
            @AuthenticationPrincipal JwtClaim claim,
            @Valid @RequestBody ReviewRequest.Create request) {
        Long reviewId = reviewService.createReview(claim.getUserId(), request);
        return ResponseEntity.status(201).body(ApiResponse.created(reviewId));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<Page<ReviewResponse.Summary>>> getReviews(
            @RequestParam String productId,
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.ok(reviewService.getReviews(productId, pageable)));
    }

    @PatchMapping("/{reviewId}")
    public ResponseEntity<ApiResponse<Void>> updateReview(
            @AuthenticationPrincipal JwtClaim claim,
            @PathVariable Long reviewId,
            @Valid @RequestBody ReviewRequest.Update request) {
        reviewService.updateReview(claim.getUserId(), reviewId, request);
        return ResponseEntity.ok(ApiResponse.ok());
    }

    @DeleteMapping("/{reviewId}")
    public ResponseEntity<ApiResponse<Void>> deleteReview(
            @AuthenticationPrincipal JwtClaim claim,
            @PathVariable Long reviewId) {
        reviewService.deleteReview(claim.getUserId(), reviewId);
        return ResponseEntity.ok(ApiResponse.ok());
    }
}
