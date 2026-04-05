package com.omisys.order.server.infrastructure.client.fallback;

import com.omisys.order.server.exception.OrderErrorCode;
import com.omisys.order.server.exception.OrderException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

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
                .hasMessageContaining("일시적으로 서비스를 이용할 수 없습니다");
    }

    @Test
    @DisplayName("updateStock fallback - SERVICE_UNAVAILABLE 예외 발생")
    void updateStock_fallback_throwsServiceUnavailable() {
        assertThatThrownBy(() -> fallback.updateStock(Map.of("p1", 1)))
                .isInstanceOf(OrderException.class)
                .hasMessageContaining("일시적으로 서비스를 이용할 수 없습니다");
    }

    @Test
    @DisplayName("rollbackStock fallback - SERVICE_UNAVAILABLE 예외 발생")
    void rollbackStock_fallback_throwsServiceUnavailable() {
        assertThatThrownBy(() -> fallback.rollbackStock(Map.of("p1", 1)))
                .isInstanceOf(OrderException.class)
                .hasMessageContaining("일시적으로 서비스를 이용할 수 없습니다");
    }
}
