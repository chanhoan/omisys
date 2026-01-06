package com.omisys.product.domain.repository.jpa;

import com.omisys.product.domain.model.PreOrder;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PreOrderRepository extends JpaRepository<PreOrder, Long> {
    Optional<PreOrder> findByPreOrderId(Long preOrderId);

    Page<PreOrder> findAllByIsPublicTrue(Pageable pageable);
}
