package com.omisys.product.domain.repository.jpa;

import com.omisys.product.domain.model.outbox.OutboxEvent;
import com.omisys.product.domain.model.outbox.OutboxStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface OutboxEventRepository extends JpaRepository<OutboxEvent, Long> {

    @Query("""
        SELECT e FROM OutboxEvent e
        WHERE e.status = :status
          AND (e.nextRetryAt IS NULL OR e.nextRetryAt <= :now)
        ORDER BY e.createdAt ASC
    """)
    List<OutboxEvent> findByStatusAndReadyToPublish(
            @Param("now") LocalDateTime now,
            @Param("status") OutboxStatus status,
            Pageable pageable);

    default List<OutboxEvent> findPendingReadyToPublish(LocalDateTime now, Pageable pageable) {
        return findByStatusAndReadyToPublish(now, OutboxStatus.PENDING, pageable);
    }
}
