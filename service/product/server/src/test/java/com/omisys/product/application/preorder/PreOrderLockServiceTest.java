package com.omisys.product.application.preorder;

import com.omisys.product.infrastructure.utils.PreOrderRedisDto;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.LocalDateTime;

import static com.omisys.product.infrastructure.utils.RedisUtils.getRedisKeyOfPreOrder;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PreOrderLockServiceTest {

    @Mock private PreOrderRedisService preOrderRedisService;
    @Mock private PreOrderCacheService preOrderCacheService;
    @Mock private DistributedLockComponent distributedLockComponent;

    private PreOrderLockService preOrderLockService;

    private final long preOrderId = 10L;
    private final long userId = 777L;

    @BeforeEach
    void setUp() {
        preOrderLockService = new PreOrderLockService(
                preOrderRedisService, preOrderCacheService, distributedLockComponent);
        TransactionSynchronizationManager.initSynchronization();

        when(preOrderCacheService.getPreOrderCache(preOrderId)).thenReturn(new PreOrderRedisDto(
                preOrderId, "product-uuid-string", 100,
                LocalDateTime.now().minusDays(1), LocalDateTime.now().plusDays(1)));
        doAnswer(inv -> {
            inv.<Runnable>getArgument(3).run();
            return null;
        }).when(distributedLockComponent).execute(anyString(), anyLong(), anyLong(), any(Runnable.class));
    }

    @AfterEach
    void tearDown() {
        TransactionSynchronizationManager.clearSynchronization();
    }

    private void completeTransaction(int status) {
        TransactionSynchronizationManager.getSynchronizations()
                .forEach(sync -> sync.afterCompletion(status));
    }

    @Test
    @DisplayName("reservation: 트랜잭션이 롤백되면(예: Outbox 저장 실패) Redis 예약을 되돌린다")
    void reservation_rollback_cancelsRedisReservation() {
        // when
        preOrderLockService.reservation(preOrderId, userId);
        completeTransaction(TransactionSynchronization.STATUS_ROLLED_BACK);

        // then
        verify(preOrderRedisService).cancel(getRedisKeyOfPreOrder(preOrderId), userId);
    }

    @Test
    @DisplayName("reservation: 트랜잭션이 커밋되면 Redis 예약을 유지한다")
    void reservation_commit_keepsRedisReservation() {
        // when
        preOrderLockService.reservation(preOrderId, userId);
        completeTransaction(TransactionSynchronization.STATUS_COMMITTED);

        // then
        verify(preOrderRedisService, never()).cancel(anyString(), anyLong());
    }
}
