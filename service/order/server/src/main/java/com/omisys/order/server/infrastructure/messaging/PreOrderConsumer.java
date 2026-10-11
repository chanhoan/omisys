package com.omisys.order.server.infrastructure.messaging;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.omisys.common.domain.entity.KafkaTopicConstant;
import com.omisys.order.order_dto.dto.OrderCreateRequest;
import com.omisys.order.server.application.service.OrderCreateService;
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

    // Outbox가 미리 직렬화한 JSON 문자열을 JsonSerializer가 다시 감싸 보내므로 String으로 받아 직접 파싱한다
    @KafkaListener(topics = KafkaTopicConstant.PROCESS_PREORDER, groupId = "product")
    public void consume(
            @Payload String payload,
            @Header(name = "kafka_receivedMessageKey") String userId) throws JsonProcessingException {

        OrderCreateRequest request = objectMapper.readValue(payload, OrderCreateRequest.class);
        log.info("preorder by {}", userId);
        orderCreateService.createOrder(Long.parseLong(userId), request);
        log.info("process preorder : {}", request.getOrderProductInfos().get(0).getProductId());

    }

}
