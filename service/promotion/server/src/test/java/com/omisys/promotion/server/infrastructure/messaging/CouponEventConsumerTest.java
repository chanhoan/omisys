package com.omisys.promotion.server.infrastructure.messaging;

import com.omisys.promotion.server.application.service.CouponInternalService;
import com.omisys.promotion.server.application.service.DistributedLockComponent;
import com.omisys.promotion.server.presentation.request.CouponRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CouponEventConsumerTest {

    @Mock private CouponInternalService couponInternalService;
    @Mock private DistributedLockComponent lockComponent;

    @InjectMocks private CouponEventConsumer consumer;

    @Test
    @DisplayName("handleCouponIssue: lockComponent.execute 호출 + Runnable 내부에서 provideEventCouponInternal 호출 보장")
    void handleCouponIssue_calls_lock_and_executes_internal_logic_inside_runnable() {
        // given
        Long userId = 1L;
        Long couponId = 10L;
        CouponRequest.Event event = new CouponRequest.Event(userId, couponId);

        /**
         * 핵심 포인트:
         * - unit test에서 '진짜 분산락'을 잡을 필요는 없음
         * - 대신 execute()가 Runnable을 실행하도록 doAnswer로 스텁하면,
         *   "락 내부에서 비즈니스 로직이 실행되는 구조"를 재현 가능
         */
        doAnswer(invocation -> {
            Runnable logic = invocation.getArgument(3, Runnable.class);
            logic.run(); // 락 내부 로직 실행
            return null;
        }).when(lockComponent).execute(anyString(), anyLong(), anyLong(), any(Runnable.class));

        // when
        consumer.handleCouponIssue(event);

        // then
        // 1) 락 호출 파라미터(락 이름/timeout) 검증
        verify(lockComponent).execute(
                eq("couponProvideLock_%s".formatted(couponId)),
                eq(3000L),
                eq(3000L),
                any(Runnable.class)
        );

        // 2) Runnable 내부에서 내부 서비스가 호출되는지 검증
        verify(couponInternalService).provideEventCouponInternal(userId, couponId);

        // (옵션) 불필요한 추가 호출이 없었는지 방어
        verifyNoMoreInteractions(couponInternalService);
    }
}
