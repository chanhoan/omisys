package com.omisys.delivery.server.infrastructure.client.fallback;

import com.omisys.delivery.server.exception.DeliveryErrorCode;
import com.omisys.delivery.server.exception.DeliveryException;
import com.omisys.delivery.server.infrastructure.client.OrderClient;
import com.omisys.order.order_dto.dto.NotificationOrderDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class OrderClientFallback implements OrderClient {

    @Override
    public NotificationOrderDto getOrder(Long orderId) {
        log.error("[CB] OrderClient.getOrder 호출 실패 - orderId={}", orderId);
        throw new DeliveryException(DeliveryErrorCode.SERVICE_UNAVAILABLE);
    }
}
