package com.omisys.delivery.server.infrastructure.repository;

import com.omisys.delivery.server.domain.model.outbox.OutboxEvent;
import com.omisys.delivery.server.domain.model.outbox.OutboxStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface OutboxEventRepository extends JpaRepository<OutboxEvent, Long> {

    @Query("SELECT e FROM OutboxEvent e WHERE e.status = :status " +
           "AND (e.nextRetryAt IS NULL OR e.nextRetryAt <= :now) " +
           "ORDER BY e.createdAt ASC")
    List<OutboxEvent> findPendingReadyToPublish(@Param("status") OutboxStatus status,
                                                @Param("now") LocalDateTime now,
                                                Pageable pageable);
}
