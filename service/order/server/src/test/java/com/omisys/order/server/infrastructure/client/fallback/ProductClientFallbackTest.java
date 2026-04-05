package com.omisys.order.server.infrastructure.client.fallback;

import com.omisys.order.server.exception.OrderErrorCode;
import com.omisys.order.server.exception.OrderException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ProductClientFallbackTest {

    private ProductClientFallback fallback;

    @BeforeEach
    void setUp() {
        fallback = new ProductClientFallback();
    }

    @Test
    @DisplayName("getProductList fallback - SERVICE_UNAVAILABLE 예외 발생")
    void getProductList_fallback_throwsServiceUnavailable() {
        assertThatThrownBy(() -> fallback.getProductList(List.of("p1", "p2")))
                .isInstanceOf(OrderException.class)
                .satisfies(ex -> assertThat(((OrderException) ex).getErrorCode())
                        .isEqualTo(OrderErrorCode.SERVICE_UNAVAILABLE));
    }

    @Test
    @DisplayName("updateStock fallback - SERVICE_UNAVAILABLE 예외 발생")
    void updateStock_fallback_throwsServiceUnavailable() {
        assertThatThrownBy(() -> fallback.updateStock(Map.of("p1", 1)))
                .isInstanceOf(OrderException.class)
                .satisfies(ex -> assertThat(((OrderException) ex).getErrorCode())
                        .isEqualTo(OrderErrorCode.SERVICE_UNAVAILABLE));
    }

    @Test
    @DisplayName("rollbackStock fallback - 보상 경로이므로 예외 없이 반환")
    void rollbackStock_fallback_noException() {
        // rollbackStock fallback은 보상 경로 — 예외 없이 로깅만 하고 반환해야 함
        fallback.rollbackStock(Map.of("p1", 1));
        // 예외가 발생하지 않으면 테스트 통과
    }
}
