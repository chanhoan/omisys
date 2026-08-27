package com.omisys.order.server.domain.repository;

import com.omisys.order.server.domain.model.Order;
import com.omisys.order.server.domain.model.vo.OrderState;
import java.time.LocalDateTime;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface OrderRepositoryCustom {

    Page<Order> getMyOrder(Pageable pageable, Long userId, String keyword);

    Page<Order> getAllOrder(Pageable pageable, Long orderUserId, String productId, OrderState state,
                             LocalDateTime from, LocalDateTime to);

}
