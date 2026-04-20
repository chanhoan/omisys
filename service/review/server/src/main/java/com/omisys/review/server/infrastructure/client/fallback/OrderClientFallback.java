package com.omisys.review.server.infrastructure.client.fallback;

import com.omisys.review.server.exception.ReviewErrorCode;
import com.omisys.review.server.exception.ReviewException;
import com.omisys.review.server.infrastructure.client.OrderClient;
import org.springframework.stereotype.Component;

@Component
public class OrderClientFallback implements OrderClient {

    @Override
    public boolean isPurchaseConfirmed(Long orderId, Long userId) {
        throw new ReviewException(ReviewErrorCode.ORDER_NOT_VERIFIABLE);
    }
}
