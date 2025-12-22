package com.omisys.promotion.server.infrastructure.messaging;

import com.omisys.common.domain.entity.KafkaTopicConstant;
import com.omisys.promotion.server.application.service.CouponInternalService;
import com.omisys.promotion.server.application.service.DistributedLockComponent;
import com.omisys.promotion.server.presentation.request.CouponRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Slf4j(topic = "CouponEventConsumer")
@RequiredArgsConstructor
@Service
public class CouponEventConsumer {

    private final CouponInternalService couponInternalService;
    private final DistributedLockComponent lockComponent;

    @KafkaListener(topics = KafkaTopicConstant.PROVIDE_EVENT_COUPON, groupId = "coupon-service-group")
    public void handleCouponIssue(CouponRequest.Event couponEvent) {
        Long userId = couponEvent.getUserId();
        Long couponId = couponEvent.getCouponId();

        log.info("provide coupon userId: {}", userId);
        lockComponent.execute(
                "couponProvideLock_%s".formatted(couponId),
                3000,
                3000,
                () -> {
                    couponInternalService.provideEventCouponInternal(userId, couponId);
                }
        );
        log.info("provide coupon couponId: {}", couponId);
    }

}