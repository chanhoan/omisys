package com.omisys.product.application.preorder;

import com.omisys.common.domain.entity.KafkaTopicConstant;
import com.omisys.order.order_dto.dto.OrderCreateRequest;
import com.omisys.order.order_dto.dto.OrderProductInfo;
import com.omisys.product.infrastructure.messaging.PreOrderProducer;
import com.omisys.product.infrastructure.utils.PreOrderRedisDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PreOrderFacadeService {

    private final PreOrderProducer preOrderProducer;
    private final PreOrderLockService preOrderLockService;

    @Transactional
    public void preOrder(long preOrderId, long addressId, long userId) {
        PreOrderRedisDto cachedData = preOrderLockService.reservation(preOrderId, userId);
        OrderCreateRequest createRequest = toDto(cachedData.productId(), addressId);
        preOrderProducer.send(
                KafkaTopicConstant.PROCESS_PREORDER, Long.toString(userId), createRequest);
    }

    private OrderCreateRequest toDto(String productId, Long addressId) {
        OrderProductInfo orderProduct = new OrderProductInfo(productId, 1, null);
        return new OrderCreateRequest("PREORDER", List.of(orderProduct), BigDecimal.ZERO, addressId);
    }

}
