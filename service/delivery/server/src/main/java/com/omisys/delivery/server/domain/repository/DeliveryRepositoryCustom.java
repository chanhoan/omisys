package com.omisys.delivery.server.domain.repository;

import com.omisys.delivery.server.domain.model.Delivery;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface DeliveryRepositoryCustom {
    Page<Delivery> getMyDelivery(Pageable pageable, Long userId);
    Page<Delivery> getAllDelivery(Pageable pageable, Long userId, String state);
}
