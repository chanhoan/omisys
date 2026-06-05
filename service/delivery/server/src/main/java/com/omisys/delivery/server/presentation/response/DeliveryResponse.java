package com.omisys.delivery.server.presentation.response;

import java.time.LocalDateTime;

public class DeliveryResponse {

    public record Get(
            Long deliveryId,
            Long orderId,
            Long userId,
            String state,
            String courier,
            String invoiceNumber,
            String recipient,
            String phoneNumber,
            String zipcode,
            String shippingAddress
    ) {}

    public record MyGet(
            Long deliveryId,
            Long orderId,
            String state,
            String courier,
            String invoiceNumber
    ) {}

    public record AdminGet(
            Long deliveryId,
            Long orderId,
            Long userId,
            String state,
            String courier,
            String invoiceNumber,
            LocalDateTime createdAt
    ) {}

    public record TrackingHistory(
            String state,
            String memo,
            LocalDateTime occurredAt
    ) {}
}
