package com.omisys.order.server.infrastructure.client.fallback;

import com.omisys.order.server.exception.OrderErrorCode;
import com.omisys.order.server.exception.OrderException;
import com.omisys.order.server.infrastructure.client.PaymentClient;
import com.omisys.payment.payment_dto.dto.PaymentInternalDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class PaymentClientFallback implements PaymentClient {

    @Override
    public void payment(PaymentInternalDto.Create createRequest) {
        log.error("[CB] PaymentClient.payment 호출 실패 - orderId={}", createRequest.getOrderId());
        throw new OrderException(OrderErrorCode.SERVICE_UNAVAILABLE);
    }

    @Override
    public void cancelPayment(PaymentInternalDto.Cancel cancelRequest) {
        log.error("[CB] PaymentClient.cancelPayment 호출 실패 - orderId={}", cancelRequest.getOrderId());
        throw new OrderException(OrderErrorCode.SERVICE_UNAVAILABLE);
    }

    @Override
    public PaymentInternalDto.Get getPayment(Long orderId) {
        log.error("[CB] PaymentClient.getPayment 호출 실패 - orderId={}", orderId);
        throw new OrderException(OrderErrorCode.SERVICE_UNAVAILABLE);
    }
}
