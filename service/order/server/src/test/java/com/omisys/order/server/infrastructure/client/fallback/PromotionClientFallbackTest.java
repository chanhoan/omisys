package com.omisys.order.server.infrastructure.client.fallback;

import com.omisys.order.server.exception.OrderErrorCode;
import com.omisys.order.server.exception.OrderException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PromotionClientFallbackTest {

    private final PromotionClientFallback fallback = new PromotionClientFallback();

    @Test
    @DisplayName("applyUserCoupon: CB open 시 SERVICE_UNAVAILABLE 예외")
    void applyUserCoupon_throws_service_unavailable() {
        assertThatThrownBy(() -> fallback.applyUserCoupon(1L, 1L, BigDecimal.valueOf(5000)))
                .isInstanceOf(OrderException.class)
                .satisfies(ex -> {
                    OrderException oe = (OrderException) ex;
                    assertThat(oe.getStatusName())
                            .isEqualTo(OrderErrorCode.SERVICE_UNAVAILABLE.getStatus().name());
                });
    }

    @Test
    @DisplayName("refundCoupon: CB open 시 예외 없이 반환 (보상 경로 — 후속 단계 계속 진행)")
    void refundCoupon_no_exception_on_cb_open() {
        // 예외를 던지지 않아야 함 — 보상 경로는 로그만 남기고 계속 진행
        fallback.refundCoupon(1L, 1L);
    }
}
