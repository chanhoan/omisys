package com.omisys.order.server.presentation.response;

public record OrderCreateResponse(Long orderId, String checkoutUrl) {

    public static OrderCreateResponse of(Long orderId, String checkoutUrl) {
        return new OrderCreateResponse(orderId, checkoutUrl);
    }
}
