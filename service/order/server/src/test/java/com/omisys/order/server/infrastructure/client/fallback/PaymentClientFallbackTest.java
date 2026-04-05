package com.omisys.order.server.infrastructure.client.fallback;

import com.omisys.order.server.exception.OrderException;
import com.omisys.payment.payment_dto.dto.PaymentInternalDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

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
                .hasMessageContaining("일시적으로 서비스를 이용할 수 없습니다");
    }

    @Test
    @DisplayName("cancelPayment fallback - SERVICE_UNAVAILABLE 예외 발생")
    void cancelPayment_fallback_throwsServiceUnavailable() {
        PaymentInternalDto.Cancel request = new PaymentInternalDto.Cancel(1L, "서비스 장애 취소");
        assertThatThrownBy(() -> fallback.cancelPayment(request))
                .isInstanceOf(OrderException.class)
                .hasMessageContaining("일시적으로 서비스를 이용할 수 없습니다");
    }

    @Test
    @DisplayName("getPayment fallback - SERVICE_UNAVAILABLE 예외 발생")
    void getPayment_fallback_throwsServiceUnavailable() {
        assertThatThrownBy(() -> fallback.getPayment(1L))
                .isInstanceOf(OrderException.class)
                .hasMessageContaining("일시적으로 서비스를 이용할 수 없습니다");
    }
}
