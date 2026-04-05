package com.omisys.order.server.infrastructure.client.fallback;

import com.omisys.order.server.exception.OrderException;
import com.omisys.user_dto.infrastructure.PointHistoryDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

class UserClientFallbackTest {

    private UserClientFallback fallback;

    @BeforeEach
    void setUp() {
        fallback = new UserClientFallback();
    }

    @Test
    @DisplayName("getUser fallback - SERVICE_UNAVAILABLE 예외 발생")
    void getUser_fallback_throwsServiceUnavailable() {
        assertThatThrownBy(() -> fallback.getUser(1L))
                .isInstanceOf(OrderException.class)
                .hasMessageContaining("일시적으로 서비스를 이용할 수 없습니다");
    }

    @Test
    @DisplayName("createPointHistory fallback - SERVICE_UNAVAILABLE 예외 발생")
    void createPointHistory_fallback_throwsServiceUnavailable() {
        PointHistoryDto request = new PointHistoryDto(1L, 1L, BigDecimal.TEN, "사용", "주문 결제");
        assertThatThrownBy(() -> fallback.createPointHistory(request))
                .isInstanceOf(OrderException.class)
                .hasMessageContaining("일시적으로 서비스를 이용할 수 없습니다");
    }

    @Test
    @DisplayName("rollbackPoint fallback - SERVICE_UNAVAILABLE 예외 발생")
    void rollbackPoint_fallback_throwsServiceUnavailable() {
        assertThatThrownBy(() -> fallback.rollbackPoint(1L))
                .isInstanceOf(OrderException.class)
                .hasMessageContaining("일시적으로 서비스를 이용할 수 없습니다");
    }

    @Test
    @DisplayName("getAddress fallback - SERVICE_UNAVAILABLE 예외 발생")
    void getAddress_fallback_throwsServiceUnavailable() {
        assertThatThrownBy(() -> fallback.getAddress(1L))
                .isInstanceOf(OrderException.class)
                .hasMessageContaining("일시적으로 서비스를 이용할 수 없습니다");
    }
}
