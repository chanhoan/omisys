package com.omisys.product.application.preorder;

import com.omisys.product.infrastructure.utils.PreOrderRedisDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import static com.omisys.product.infrastructure.utils.RedisUtils.getRedisKeyOfPreOrder;

@Slf4j
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
                    // 수량 확인과 차감(SADD)을 같은 락 안에서 수행해야 초과 예약이 생기지 않는다
                    preOrderRedisService.validateQuantity(cachedPreOrder, userId);
                    preOrderRedisService.preOrder(getRedisKeyOfPreOrder(preOrderId), userId);
                });
        cancelReservationOnRollback(preOrderId, userId);
        return cachedPreOrder;
    }

    // SADD는 DB 트랜잭션에 묶이지 않으므로, Outbox 저장·커밋이 실패하면 예약 슬롯을 직접 되돌린다
    private void cancelReservationOnRollback(long preOrderId, long userId) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCompletion(int status) {
                if (status == STATUS_COMMITTED) {
                    return;
                }
                try {
                    preOrderRedisService.cancel(getRedisKeyOfPreOrder(preOrderId), userId);
                } catch (RuntimeException e) {
                    log.error("사전예약 Redis 보상 실패 preOrderId={}, userId={}", preOrderId, userId, e);
                }
            }
        });
    }
}
