package com.omisys.product.application.preorder;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.omisys.common.domain.entity.KafkaTopicConstant;
import com.omisys.order.order_dto.dto.OrderCreateRequest;
import com.omisys.order.order_dto.dto.OrderProductInfo;
import com.omisys.product.domain.model.outbox.OutboxEvent;
import com.omisys.product.domain.repository.jpa.OutboxEventRepository;
import com.omisys.product.infrastructure.utils.PreOrderRedisDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PreOrderFacadeService {

    private final PreOrderLockService preOrderLockService;
    private final OutboxEventRepository outboxEventRepository;
    private final ObjectMapper objectMapper;

    /**
     * 사전예약 처리.
     * reservation()과 OutboxEvent 저장을 같은 트랜잭션 내에서 원자적으로 수행한다.
     * Kafka 발행은 OutboxEventPoller가 별도로 담당한다.
     */
    @Transactional
    public void preOrder(long preOrderId, long addressId, long userId) {
        PreOrderRedisDto cachedData = preOrderLockService.reservation(preOrderId, userId);
        OrderCreateRequest request = toDto(cachedData.productId(), addressId);
        outboxEventRepository.save(OutboxEvent.pending(
                "PreOrder",
                String.valueOf(preOrderId),
                KafkaTopicConstant.PROCESS_PREORDER,
                Long.toString(userId),
                toJson(request)
        ));
    }

    private OrderCreateRequest toDto(String productId, Long addressId) {
        OrderProductInfo orderProduct = new OrderProductInfo(productId, 1, null);
        return new OrderCreateRequest("PREORDER", List.of(orderProduct), BigDecimal.ZERO, addressId);
    }

    private String toJson(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("OutboxEvent payload 직렬화 실패", e);
        }
    }
}
