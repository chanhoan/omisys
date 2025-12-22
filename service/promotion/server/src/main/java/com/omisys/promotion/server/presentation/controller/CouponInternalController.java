package com.omisys.promotion.server.presentation.controller;

import com.omisys.promotion.server.application.service.CouponInternalService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/internal/coupons")
public class CouponInternalController {

    private final CouponInternalService couponInternalService;

    @PatchMapping("/{couponId}/user")
    public void userCoupon(
            @PathVariable(name = "couponId") Long couponId,
            @RequestParam(name = "userId") Long userId) {
        couponInternalService.useCoupon(couponId, userId);
    }

    @PatchMapping("/{couponId}/refund")
    public void refundCoupon(
            @PathVariable(name = "couponId") Long couponId,
            @RequestParam(name = "userId") Long userId) {
        couponInternalService.refundCoupon(couponId, userId);
    }

}
