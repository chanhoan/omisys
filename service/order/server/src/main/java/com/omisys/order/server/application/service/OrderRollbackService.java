package com.omisys.order.server.application.service;

import com.omisys.order.server.infrastructure.client.ProductClient;
import com.omisys.order.server.infrastructure.client.UserClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
@Slf4j(topic = "OrderRollbackService")
@RequiredArgsConstructor
public class OrderRollbackService {

    private final UserClient userClient;
    private final ProductClient productClient;

    public void rollbackTransaction(
            Map<String, Integer> deductedProductsQuantities,
            List<Long> usedCoupons,
            Long pointHistoryId) {

        if (deductedProductsQuantities != null && !deductedProductsQuantities.isEmpty()) {
            try {
                productClient.rollbackStock(deductedProductsQuantities);
            } catch (Exception e) {
                // 보상 실패 시 수동 개입 필요 — 재throw하지 않고 다음 보상 단계를 계속 진행
                log.error("[COMPENSATION-FAIL] 재고 롤백 실패 — 수동 복구 필요. quantities={}", deductedProductsQuantities, e);
            }
        }

        if (usedCoupons != null && !usedCoupons.isEmpty()) {
            // TODO usedCoupons 롤백 feign 호출
            log.warn("[COMPENSATION-SKIP] 쿠폰 롤백 미구현 — 수동 확인 필요. couponIds={}", usedCoupons);
        }

        if (pointHistoryId != null) {
            try {
                userClient.rollbackPoint(pointHistoryId);
            } catch (Exception e) {
                log.error("[COMPENSATION-FAIL] 포인트 롤백 실패 — 수동 복구 필요. pointHistoryId={}", pointHistoryId, e);
            }
        }
    }

}
