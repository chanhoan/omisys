package com.omisys.delivery.server.infrastructure.client;

import com.omisys.delivery.server.infrastructure.client.fallback.OrderClientFallback;
import com.omisys.delivery.server.infrastructure.configuration.InternalSecretFeignConfig;
import com.omisys.order.order_dto.dto.NotificationOrderDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "order", fallback = OrderClientFallback.class, configuration = InternalSecretFeignConfig.class)
public interface OrderClient {

    @GetMapping("/internal/orders")
    NotificationOrderDto getOrder(@RequestParam Long orderId);
}
