package com.omisys.order.server.domain.repository;

import com.omisys.order.server.domain.model.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long>, OrderRepositoryCustom {

    boolean existsByOrderNo(String orderNo);

    @Query("SELECT o FROM Order o LEFT JOIN FETCH o.orderProducts WHERE o.orderId = :orderId")
    Optional<Order> findById(Long orderId);

}
