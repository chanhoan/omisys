package com.omisys.delivery.server.domain.repository;

import com.omisys.delivery.server.domain.model.Delivery;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface DeliveryRepository extends JpaRepository<Delivery, Long>, DeliveryRepositoryCustom {
    Optional<Delivery> findByOrderId(Long orderId);
    boolean existsByOrderId(Long orderId);
}
