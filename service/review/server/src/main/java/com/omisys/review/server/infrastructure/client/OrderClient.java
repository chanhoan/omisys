package com.omisys.review.server.infrastructure.client;

import com.omisys.review.server.infrastructure.client.fallback.OrderClientFallback;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "order-service", fallback = OrderClientFallback.class)
public interface OrderClient {

    @GetMapping("/internal/orders/purchase-confirmed")
    boolean isPurchaseConfirmed(@RequestParam Long orderId, @RequestParam Long userId);
}
