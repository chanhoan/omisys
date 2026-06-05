package com.omisys.delivery.server.domain.repository;

import com.omisys.delivery.server.domain.model.Delivery;
import com.omisys.delivery.server.domain.model.DeliveryTrackingHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DeliveryTrackingHistoryRepository extends JpaRepository<DeliveryTrackingHistory, Long> {
    List<DeliveryTrackingHistory> findByDeliveryOrderByOccurredAtDesc(Delivery delivery);
}
