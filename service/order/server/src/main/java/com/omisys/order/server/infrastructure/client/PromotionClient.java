package com.omisys.order.server.infrastructure.client;

import com.omisys.order.server.infrastructure.client.fallback.PromotionClientFallback;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.math.BigDecimal;

@FeignClient(name = "promotion", fallback = PromotionClientFallback.class)
public interface PromotionClient {

    @PatchMapping("/internal/coupons/{couponId}/apply")
    BigDecimal applyUserCoupon(
            @PathVariable(name = "couponId") Long couponId,
            @RequestParam(name = "userId") Long userId,
            @RequestParam(name = "productPrice") BigDecimal productPrice);

    @PatchMapping("/internal/coupons/{couponId}/refund")
    void refundCoupon(
            @PathVariable(name = "couponId") Long couponId,
            @RequestParam(name = "userId") Long userId);

}
