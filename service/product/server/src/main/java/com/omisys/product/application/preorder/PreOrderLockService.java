package com.omisys.product.application.preorder;

import com.omisys.product.infrastructure.utils.PreOrderRedisDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import static com.omisys.product.infrastructure.utils.RedisUtils.getRedisKeyOfPreOrder;

@Service
@RequiredArgsConstructor
public class PreOrderLockService {

    private final PreOrderRedisService preOrderRedisService;
    private final PreOrderCacheService preOrderCacheService;
    private final DistributedLockComponent distributedLockComponent;

    @Transactional
    public PreOrderRedisDto reservation(long preOrderId, long userId) {
        PreOrderRedisDto cachedPreOrder = preOrderCacheService.getPreOrderCache(preOrderId);
        cachedPreOrder.validateReservationDate();
        distributedLockComponent.execute(
                "preOrderLock_%s".formatted(preOrderId),
                3000,
                3000,
                () -> {
                    preOrderRedisService.validateQuantity(cachedPreOrder, userId);
                });
        preOrderRedisService.preOrder(getRedisKeyOfPreOrder(preOrderId), userId);
        return cachedPreOrder;
    }
}
