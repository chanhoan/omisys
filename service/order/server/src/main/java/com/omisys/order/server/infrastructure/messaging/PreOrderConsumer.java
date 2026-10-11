package com.omisys.order.server.infrastructure.messaging;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.omisys.common.domain.entity.KafkaTopicConstant;
import com.omisys.order.order_dto.dto.OrderCreateRequest;
import com.omisys.order.server.application.service.OrderCreateService;
import com.omisys.order.server.domain.model.vo.OrderType;
import com.omisys.order.server.domain.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j(topic = "PreOrderConsumer")
public class PreOrderConsumer {

    public final OrderCreateService orderCreateService;
    private final ObjectMapper objectMapper;
    private final OrderRepository orderRepository;

    // Outbox가 미리 직렬화한 JSON 문자열을 JsonSerializer가 다시 감싸 보내므로 String으로 받아 직접 파싱한다
    @KafkaListener(topics = KafkaTopicConstant.PROCESS_PREORDER, groupId = "product")
    public void consume(
            @Payload String payload,
            @Header(name = "kafka_receivedMessageKey") String userId) throws JsonProcessingException {

        OrderCreateRequest request = objectMapper.readValue(payload, OrderCreateRequest.class);
        long parsedUserId = Long.parseLong(userId);
        String productId = request.getOrderProductInfos().get(0).getProductId();
        // Outbox는 최소 1회 전달이라 같은 이벤트가 다시 올 수 있다. 사전예약은 사용자당 상품 1건이므로 이미 있으면 건너뛴다.
        // 같은 사용자의 메시지는 key(userId)가 같아 한 파티션에서 순서대로 처리되므로 확인-생성 사이 경쟁이 없다
        if (orderRepository.existsByUserIdAndTypeAndOrderProducts_ProductId(parsedUserId, OrderType.PREORDER, productId)) {
            log.warn("duplicate preorder event skipped: userId={} productId={}", userId, productId);
            return;
        }
        log.info("preorder by {}", userId);
        orderCreateService.createOrder(parsedUserId, request);
        log.info("process preorder : {}", productId);
    }

}
