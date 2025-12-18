package com.omisys.order.server.presentation.controller;

import com.omisys.order.order_dto.dto.NotificationOrderDto;
import com.omisys.order.server.application.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/internal/orders")
@RequiredArgsConstructor
public class OrderInternalController {

    private final OrderService orderService;

    @GetMapping
    public NotificationOrderDto getOrder(@RequestParam Long orderId) {
        return orderService.getNotificationOrder(orderId);
    }

}
