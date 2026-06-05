package com.omisys.delivery.server.application.service;

import com.omisys.delivery.server.domain.model.Delivery;
import com.omisys.delivery.server.domain.repository.DeliveryRepository;
import com.omisys.delivery.server.infrastructure.event.PaymentCompletedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j(topic = "DeliveryCreateService")
@Service
@RequiredArgsConstructor
@Transactional
public class DeliveryCreateService {

    private final DeliveryRepository deliveryRepository;

    public Long createDelivery(PaymentCompletedEvent event) {
        if (!Boolean.TRUE.equals(event.getSuccess())) {
            log.info("Payment not successful, skip delivery creation. orderId={}", event.getOrderId());
            return null;
        }

        if (deliveryRepository.existsByOrderId(event.getOrderId())) {
            log.warn("Delivery already exists for orderId={}, skipping (idempotent)", event.getOrderId());
            return null;
        }

        Delivery delivery = Delivery.create(event.getOrderId(), event.getUserId());
        Delivery saved = deliveryRepository.save(delivery);

        log.info("Delivery created: deliveryId={} orderId={} userId={}",
                saved.getDeliveryId(), saved.getOrderId(), saved.getUserId());
        return saved.getDeliveryId();
    }
}
