package com.omisys.order.server.application.service.mapper;

import com.omisys.order.order_dto.dto.NotificationOrderDto;
import com.omisys.order.server.domain.model.Order;

public class OrderMapper {

    public static NotificationOrderDto toNotificationOrderDto(
            Order order,
            String displayProductName) {
        return new NotificationOrderDto(order.getOrderId(), order.getUserId(),
                order.getState().getDescription(), displayProductName, order.getTotalQuantity());
    }

}
