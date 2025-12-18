package com.omisys.order.server.domain.repository;

import com.omisys.order.server.domain.model.Order;
import com.omisys.order.server.domain.model.OrderProduct;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OrderProductRepository extends JpaRepository<OrderProduct, Long> {

    List<OrderProduct> findByOrder(Order order);

}
