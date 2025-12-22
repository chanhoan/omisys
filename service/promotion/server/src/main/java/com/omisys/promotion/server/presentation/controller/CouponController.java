package com.omisys.promotion.server.presentation.controller;

import com.omisys.auth.server.auth_dto.jwt.JwtClaim;
import com.omisys.common.domain.response.ApiResponse;
import com.omisys.promotion.server.application.service.CouponService;
import com.omisys.promotion.server.presentation.request.CouponRequest;
import com.omisys.promotion.server.presentation.response.CouponResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/coupons")
public class CouponController {

    private final CouponService couponService;

    @PreAuthorize("hasAnyRole('ROLE_MANAGER', 'ROLE_ADMIN')")
    @PostMapping("/event")
    public ApiResponse<Void> createEventCoupon(
            @RequestBody @Valid CouponRequest.Create request) {
        couponService.createEventCoupon(request);
        return ApiResponse.ok();
    }

    @PreAuthorize("hasAnyRole('ROLE_USER', 'ROLE_MANAGER', 'ROLE_ADMIN')")
    @PostMapping("/event/{couponId}")
    public ApiResponse<Map<String, String>> provideEventCoupon(
            @PathVariable(name = "couponId") Long couponId,
            @AuthenticationPrincipal JwtClaim claim
    ) {
        couponService.provideEventCouponRequest(claim.getUserId(), couponId);
        return ApiResponse.ok(Map.of("message", "쿠폰 발급 중입니다."));
    }

    @PreAuthorize("hasAnyRole('ROLE_MANAGER', 'ROLE_ADMIN')")
    @GetMapping
    public ApiResponse<Page<CouponResponse.Get>> getCouponList(Pageable pageable) {
        return ApiResponse.ok(couponService.getCouponList(pageable));
    }

    @PreAuthorize("hasAnyRole('ROLE_USER', 'ROLE_MANAGER', 'ROLE_ADMIN')")
    @GetMapping("/{couponId}")
    public ApiResponse<CouponResponse.Get> getCoupon(@PathVariable(name = "couponId") Long couponId) {
        return ApiResponse.ok(couponService.getCoupon(couponId));
    }

    @PreAuthorize("hasAnyRole('ROLE_MANAGER', 'ROLE_ADMIN')")
    @GetMapping("/users/{userId}")
    public ApiResponse<Page<CouponResponse.Get>> getCouponListByUserId(
            @PathVariable(name = "userId") Long userId, Pageable pageable) {
        return ApiResponse.ok(couponService.getCouponListBoyUserId(userId, pageable));
    }

    @PreAuthorize("hasRole('ROLE_USER')")
    @GetMapping("/me")
    public ApiResponse<Page<CouponResponse.Get>> getCouponListByUser(
            @AuthenticationPrincipal JwtClaim jwtClaim, Pageable pageable) {
        return ApiResponse.ok(couponService.getCouponListBoyUserId(jwtClaim.getUserId(), pageable));
    }

    @PreAuthorize("hasAnyRole('ROLE_MANAGER', 'ROLE_ADMIN')")
    @PatchMapping("/{couponId}")
    public ApiResponse<?> updateCoupon(
            @PathVariable(name = "couponId") Long couponId,
            @RequestBody @Valid CouponRequest.Update request) {
        couponService.updateCoupon(couponId, request);
        return ApiResponse.ok();
    }

    @PreAuthorize("hasAnyRole('ROLE_MANAGER', 'ROLE_ADMIN')")
    @DeleteMapping("/{couponId}")
    public ApiResponse<?> deleteCoupon(@PathVariable(name = "couponId") Long couponId) {
        couponService.deleteCoupon(couponId);
        return ApiResponse.ok();
    }
}
