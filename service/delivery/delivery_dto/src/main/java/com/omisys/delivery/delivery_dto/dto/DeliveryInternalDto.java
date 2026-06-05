package com.omisys.delivery.delivery_dto.dto;

public class DeliveryInternalDto {

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
}
