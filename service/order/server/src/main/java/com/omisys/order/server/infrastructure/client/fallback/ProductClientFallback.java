package com.omisys.order.server.infrastructure.client.fallback;

import com.omisys.order.server.exception.OrderErrorCode;
import com.omisys.order.server.exception.OrderException;
import com.omisys.order.server.infrastructure.client.ProductClient;
import com.omisys.product.product_dto.ProductDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Slf4j
@Component
public class ProductClientFallback implements ProductClient {

    @Override
    public List<ProductDto> getProductList(List<String> productIds) {
        log.error("[CB] ProductClient.getProductList 호출 실패 - productIds={}", productIds);
        throw new OrderException(OrderErrorCode.SERVICE_UNAVAILABLE);
    }

    @Override
    public void updateStock(Map<String, Integer> productQuantities) {
        log.error("[CB] ProductClient.updateStock 호출 실패 - quantities={}", productQuantities);
        throw new OrderException(OrderErrorCode.SERVICE_UNAVAILABLE);
    }

    @Override
    public void rollbackStock(Map<String, Integer> productQuantities) {
        // 보상 경로: 예외를 던지면 OrderRollbackService의 후속 보상 단계가 중단됨
        // 로깅만 하고 반환 — 상위 OrderRollbackService에서 COMPENSATION-FAIL 로그로 기록됨
        log.error("[CB] ProductClient.rollbackStock 호출 실패 (CB open) — 수동 복구 필요. quantities={}", productQuantities);
    }
}
