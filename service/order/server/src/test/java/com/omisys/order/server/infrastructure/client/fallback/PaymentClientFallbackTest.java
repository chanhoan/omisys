package com.omisys.order.server.infrastructure.client.fallback;

import com.omisys.order.server.exception.OrderErrorCode;
import com.omisys.order.server.exception.OrderException;
import com.omisys.payment.payment_dto.dto.PaymentInternalDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PaymentClientFallbackTest {

    private PaymentClientFallback fallback;

    @BeforeEach
    void setUp() {
        fallback = new PaymentClientFallback();
    }

    @Test
    @DisplayName("payment fallback - SERVICE_UNAVAILABLE 예외 발생")
    void payment_fallback_throwsServiceUnavailable() {
        PaymentInternalDto.Create request = new PaymentInternalDto.Create(1L, 1L, "ORD001", "test@test.com", 10000L);
        assertThatThrownBy(() -> fallback.payment(request))
                .isInstanceOf(OrderException.class)
                .satisfies(ex -> assertThat(((OrderException) ex).getErrorCode())
                        .isEqualTo(OrderErrorCode.SERVICE_UNAVAILABLE));
    }

    @Test
    @DisplayName("cancelPayment fallback - 보상 경로이므로 예외 없이 반환")
    void cancelPayment_fallback_noException() {
        // cancelPayment fallback은 보상 경로 — 예외 없이 로깅만 하고 반환해야 함
        PaymentInternalDto.Cancel request = new PaymentInternalDto.Cancel(1L, "서비스 장애 취소");
        fallback.cancelPayment(request);
        // 예외가 발생하지 않으면 테스트 통과
    }

    @Test
    @DisplayName("getPayment fallback - SERVICE_UNAVAILABLE 예외 발생")
    void getPayment_fallback_throwsServiceUnavailable() {
        assertThatThrownBy(() -> fallback.getPayment(1L))
                .isInstanceOf(OrderException.class)
                .satisfies(ex -> assertThat(((OrderException) ex).getErrorCode())
                        .isEqualTo(OrderErrorCode.SERVICE_UNAVAILABLE));
    }
}
