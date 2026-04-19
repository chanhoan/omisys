package com.omisys.order.server.infrastructure.client.fallback;

import com.omisys.order.server.exception.OrderErrorCode;
import com.omisys.order.server.exception.OrderException;
import com.omisys.order.server.infrastructure.client.PromotionClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Slf4j
@Component
public class PromotionClientFallback implements PromotionClient {

    @Override
    public BigDecimal applyUserCoupon(Long couponId, Long userId, BigDecimal productPrice) {
        log.error("[CB] PromotionClient.applyUserCoupon 호출 실패 - couponId={}, userId={}", couponId, userId);
        throw new OrderException(OrderErrorCode.SERVICE_UNAVAILABLE);
    }

    @Override
    public void refundCoupon(Long couponId, Long userId) {
        // 보상 경로: 예외를 던지면 OrderRollbackService의 후속 보상 단계가 중단됨
        // 로깅만 하고 반환 — 상위 OrderRollbackService에서 COMPENSATION-FAIL 로그로 기록됨
        log.error("[CB] PromotionClient.refundCoupon 호출 실패 (CB open) — 수동 복구 필요. couponId={}, userId={}", couponId, userId);
    }

}
