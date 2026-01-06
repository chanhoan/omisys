package com.omisys.product.application.preorder;

import com.omisys.product.domain.model.PreOrder;
import com.omisys.product.domain.model.PreOrderState;
import com.omisys.product.domain.repository.jpa.PreOrderRepository;
import com.omisys.product.exception.ProductErrorCode;
import com.omisys.product.exception.ProductException;
import com.omisys.product.infrastructure.utils.PreOrderRedisDto;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PreOrderCacheService {

    private final PreOrderRepository preOrderRepository;

    @Cacheable(cacheNames = "preOrder", key = "#preOrderId")
    public PreOrderRedisDto getPreOrderCache(long preOrderId) {
        PreOrder preOrder = getPreOrderByPreOrderId(preOrderId);
        validatePreOrder(preOrder);
        return new PreOrderRedisDto(preOrder);
    }

    private PreOrder getPreOrderByPreOrderId(long preOrderId) {
        return preOrderRepository
                .findByPreOrderId(preOrderId)
                .orElseThrow(() -> new ProductException(ProductErrorCode.NOT_FOUND_PREORDER));
    }

    private void validatePreOrder(PreOrder preOrder) {
        if (preOrder.getState() != PreOrderState.OPEN_FOR_ORDER) {
            throw new ProductException(ProductErrorCode.NOT_OPEN_FOR_PREORDER);
        }
    }

}
