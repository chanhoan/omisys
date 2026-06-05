package com.omisys.delivery.server.application.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.omisys.common.domain.entity.KafkaTopicConstant;
import com.omisys.delivery.server.application.service.mapper.DeliveryMapper;
import com.omisys.delivery.server.domain.model.Delivery;
import com.omisys.delivery.server.domain.model.DeliveryTrackingHistory;
import com.omisys.delivery.server.domain.model.outbox.OutboxEvent;
import com.omisys.delivery.server.domain.model.vo.Courier;
import com.omisys.delivery.server.domain.model.vo.DeliveryState;
import com.omisys.delivery.server.domain.repository.DeliveryRepository;
import com.omisys.delivery.server.domain.repository.DeliveryTrackingHistoryRepository;
import com.omisys.delivery.server.exception.DeliveryErrorCode;
import com.omisys.delivery.server.exception.DeliveryException;
import com.omisys.delivery.server.infrastructure.repository.OutboxEventRepository;
import com.omisys.delivery.server.presentation.response.DeliveryResponse;
import com.omisys.order.order_dto.dto.NotificationOrderDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j(topic = "DeliveryService")
@Service
@RequiredArgsConstructor
@Transactional
public class DeliveryService {

    private final DeliveryRepository deliveryRepository;
    private final DeliveryTrackingHistoryRepository trackingHistoryRepository;
    private final OutboxEventRepository outboxEventRepository;
    private final ObjectMapper objectMapper;

    @Transactional(readOnly = true)
    public DeliveryResponse.Get getDelivery(Long userId, Long deliveryId) {
        Delivery delivery = findDelivery(deliveryId);
        validatePermission(userId, delivery);
        return DeliveryMapper.toGetResponse(delivery);
    }

    @Transactional(readOnly = true)
    public Page<DeliveryResponse.MyGet> getMyDelivery(Long userId, Pageable pageable) {
        return deliveryRepository.getMyDelivery(pageable, userId)
                .map(DeliveryMapper::toMyGetResponse);
    }

    @Transactional(readOnly = true)
    public Page<DeliveryResponse.AdminGet> getAllDelivery(Pageable pageable, Long userId, String state) {
        return deliveryRepository.getAllDelivery(pageable, userId, state)
                .map(DeliveryMapper::toAdminGetResponse);
    }

    @Transactional(readOnly = true)
    public List<DeliveryResponse.TrackingHistory> getTrackingHistory(Long userId, Long deliveryId) {
        Delivery delivery = findDelivery(deliveryId);
        validatePermission(userId, delivery);
        return trackingHistoryRepository.findByDeliveryOrderByOccurredAtDesc(delivery)
                .stream()
                .map(DeliveryMapper::toTrackingResponse)
                .toList();
    }

    public Long registerInvoice(Long deliveryId, String courierCode, String invoiceNumber) {
        Delivery delivery = findDelivery(deliveryId);
        Courier courier = Courier.valueOf(courierCode);
        delivery.startShipping(courier, invoiceNumber);
        saveTrackingAndOutbox(delivery, DeliveryState.SHIPPING, "송장 등록: " + invoiceNumber);
        return delivery.getDeliveryId();
    }

    public Long updateState(Long deliveryId, String stateCode) {
        Delivery delivery = findDelivery(deliveryId);
        DeliveryState next = DeliveryState.valueOf(stateCode);

        switch (next) {
            case DELIVERED -> delivery.complete();
            case CANCELED -> delivery.cancel();
            default -> throw new DeliveryException(DeliveryErrorCode.INVALID_DELIVERY_STATE_TRANSITION,
                    delivery.getState().name() + " -> " + next.name() + " (SHIPPING 전이는 /invoice 엔드포인트 사용)");
        }

        saveTrackingAndOutbox(delivery, next, null);
        return delivery.getDeliveryId();
    }

    @Transactional(readOnly = true)
    public DeliveryResponse.Get getDeliveryByOrderId(Long orderId) {
        Delivery delivery = deliveryRepository.findByOrderId(orderId)
                .orElseThrow(() -> new DeliveryException(DeliveryErrorCode.DELIVERY_NOT_FOUND));
        return DeliveryMapper.toGetResponse(delivery);
    }

    private Delivery findDelivery(Long deliveryId) {
        return deliveryRepository.findById(deliveryId)
                .orElseThrow(() -> new DeliveryException(DeliveryErrorCode.DELIVERY_NOT_FOUND));
    }

    private void validatePermission(Long userId, Delivery delivery) {
        if (!delivery.getUserId().equals(userId)) {
            throw new DeliveryException(DeliveryErrorCode.DELIVERY_PERMISSION_DENIED);
        }
    }

    private void saveTrackingAndOutbox(Delivery delivery, DeliveryState state, String memo) {
        DeliveryTrackingHistory history = DeliveryTrackingHistory.of(delivery, state, memo);
        trackingHistoryRepository.save(history);

        try {
            NotificationOrderDto payload = new NotificationOrderDto(
                    delivery.getOrderId(), delivery.getUserId(), state.name(), null, null);
            String payloadJson = objectMapper.writeValueAsString(payload);
            OutboxEvent outbox = OutboxEvent.pending(
                    "Delivery",
                    delivery.getDeliveryId().toString(),
                    KafkaTopicConstant.ORDER_STATUS_CHANGED,
                    delivery.getDeliveryId().toString(),
                    payloadJson);
            outboxEventRepository.save(outbox);
        } catch (JsonProcessingException e) {
            log.error("Outbox payload serialization failed for deliveryId={}", delivery.getDeliveryId(), e);
            throw new RuntimeException("Outbox payload serialization failed", e);
        }
    }
}
