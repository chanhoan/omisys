package com.omisys.delivery.server.presentation.controller;

import com.omisys.delivery.server.application.service.DeliveryService;
import com.omisys.delivery.server.presentation.response.DeliveryResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/internal/deliveries")
@RequiredArgsConstructor
public class DeliveryInternalController {

    private final DeliveryService deliveryService;

    @GetMapping
    public DeliveryResponse.Get getDeliveryByOrderId(@RequestParam Long orderId) {
        return deliveryService.getDeliveryByOrderId(orderId);
    }
}
