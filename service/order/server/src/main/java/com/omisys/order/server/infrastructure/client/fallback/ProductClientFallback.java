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
        log.error("[CB] ProductClient.rollbackStock 호출 실패 - quantities={}", productQuantities);
        throw new OrderException(OrderErrorCode.SERVICE_UNAVAILABLE);
    }
}
