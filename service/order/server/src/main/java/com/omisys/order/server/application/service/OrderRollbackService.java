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
        try {
            if (deductedProductsQuantities != null && !deductedProductsQuantities.isEmpty()) {
                productClient.rollbackStock(deductedProductsQuantities);
            }
            if (usedCoupons != null && !usedCoupons.isEmpty()) {
                // TODO usedCoupons 에 대한 롤백 feign 호출
            }
            if (pointHistoryId != null) {
                userClient.rollbackPoint(pointHistoryId);
            }
        } catch (Exception e) {
            log.info("===== 보상 트랜잭션 처리 중, 예외 발생 =====");
            throw new RuntimeException(e);
        }
    }

}
