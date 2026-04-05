package com.omisys.order.server.infrastructure.client.fallback;

import com.omisys.order.server.exception.OrderErrorCode;
import com.omisys.order.server.exception.OrderException;
import com.omisys.user_dto.infrastructure.PointHistoryDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
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
                .satisfies(ex -> assertThat(((OrderException) ex).getErrorCode())
                        .isEqualTo(OrderErrorCode.SERVICE_UNAVAILABLE));
    }

    @Test
    @DisplayName("createPointHistory fallback - SERVICE_UNAVAILABLE 예외 발생")
    void createPointHistory_fallback_throwsServiceUnavailable() {
        PointHistoryDto request = new PointHistoryDto(1L, 1L, BigDecimal.TEN, "사용", "주문 결제");
        assertThatThrownBy(() -> fallback.createPointHistory(request))
                .isInstanceOf(OrderException.class)
                .satisfies(ex -> assertThat(((OrderException) ex).getErrorCode())
                        .isEqualTo(OrderErrorCode.SERVICE_UNAVAILABLE));
    }

    @Test
    @DisplayName("rollbackPoint fallback - 보상 경로이므로 예외 없이 반환")
    void rollbackPoint_fallback_noException() {
        // rollbackPoint fallback은 보상 경로 — 예외 없이 로깅만 하고 반환해야 함
        fallback.rollbackPoint(1L);
        // 예외가 발생하지 않으면 테스트 통과
    }

    @Test
    @DisplayName("getAddress fallback - SERVICE_UNAVAILABLE 예외 발생")
    void getAddress_fallback_throwsServiceUnavailable() {
        assertThatThrownBy(() -> fallback.getAddress(1L))
                .isInstanceOf(OrderException.class)
                .satisfies(ex -> assertThat(((OrderException) ex).getErrorCode())
                        .isEqualTo(OrderErrorCode.SERVICE_UNAVAILABLE));
    }
}
