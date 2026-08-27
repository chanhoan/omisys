package com.omisys.order.server.domain.repository;

import static com.omisys.order.server.domain.model.QOrder.order;
import static com.omisys.order.server.domain.model.QOrderProduct.orderProduct;

import com.omisys.order.server.domain.model.Order;
import com.omisys.order.server.domain.model.vo.OrderState;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.jpa.impl.JPAQueryFactory;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

@RequiredArgsConstructor
public class OrderRepositoryImpl implements OrderRepositoryCustom {

    private final JPAQueryFactory queryFactory;

    @Override
    public Page<Order> getMyOrder(Pageable pageable, Long userId, String keyword) {
        BooleanBuilder predicate = new BooleanBuilder(order.userId.eq(userId));
        if (keyword != null && !keyword.trim().isEmpty()) {
            predicate.and(orderProduct.productName.containsIgnoreCase(keyword));
        }

        List<Order> orders = queryFactory.selectFrom(order)
                .join(order.orderProducts, orderProduct).fetchJoin()
                .where(predicate)
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        Long totalSize = queryFactory.select(order.count())
                .from(order)
                .join(order.orderProducts, orderProduct)
                .where(predicate)
                .fetchOne();

        long totalElements = (totalSize != null) ? totalSize : 0;
        return new PageImpl<>(orders, pageable, totalElements);
    }

    @Override
    public Page<Order> getAllOrder(Pageable pageable, Long orderUserId, String productId, OrderState state,
                                    LocalDateTime from, LocalDateTime to) {
        BooleanBuilder predicate = new BooleanBuilder();
        if (orderUserId != null) predicate.and(order.userId.eq(orderUserId));
        if (productId != null && !productId.trim().isEmpty()) predicate.and(orderProduct.productId.eq(productId));
        if (state != null) predicate.and(order.state.eq(state));
        if (from != null) predicate.and(order.createdAt.goe(from));
        if (to != null) predicate.and(order.createdAt.loe(to));

        List<Order> orders = queryFactory.selectFrom(order)
                .join(order.orderProducts, orderProduct).fetchJoin()
                .where(predicate)
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        Long totalSize = queryFactory.select(order.count())
                .from(order)
                .join(order.orderProducts, orderProduct)
                .where(predicate)
                .fetchOne();

        long totalElements = (totalSize != null) ? totalSize : 0;
        return new PageImpl<>(orders, pageable, totalElements);
    }
}