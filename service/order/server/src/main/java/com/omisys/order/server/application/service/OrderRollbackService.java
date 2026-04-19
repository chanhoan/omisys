package com.omisys.order.server.application.service;

import com.omisys.order.server.infrastructure.client.ProductClient;
import com.omisys.order.server.infrastructure.client.PromotionClient;
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
    private final PromotionClient promotionClient;

    public void rollbackTransaction(
            Long userId,
            Map<String, Integer> deductedProductsQuantities,
            List<Long> usedCoupons,
            Long pointHistoryId) {

        if (deductedProductsQuantities != null && !deductedProductsQuantities.isEmpty()) {
            try {
                productClient.rollbackStock(deductedProductsQuantities);
            } catch (Exception e) {
                log.error("[COMPENSATION-FAIL] 재고 롤백 실패 — 수동 복구 필요. quantities={}", deductedProductsQuantities, e);
            }
        }

        if (usedCoupons != null && !usedCoupons.isEmpty()) {
            usedCoupons.forEach(couponId -> {
                try {
                    promotionClient.refundCoupon(couponId, userId);
                } catch (Exception e) {
                    log.error("[COMPENSATION-FAIL] 쿠폰 롤백 실패 — 수동 복구 필요. couponId={}, userId={}", couponId, userId, e);
                }
            });
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
