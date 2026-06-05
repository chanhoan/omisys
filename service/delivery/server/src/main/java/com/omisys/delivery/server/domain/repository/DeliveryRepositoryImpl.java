package com.omisys.delivery.server.domain.repository;

import static com.omisys.delivery.server.domain.model.QDelivery.delivery;

import com.omisys.delivery.server.domain.model.Delivery;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.util.List;

@RequiredArgsConstructor
public class DeliveryRepositoryImpl implements DeliveryRepositoryCustom {

    private final JPAQueryFactory queryFactory;

    @Override
    public Page<Delivery> getMyDelivery(Pageable pageable, Long userId) {
        JPAQuery<Delivery> query = queryFactory.selectFrom(delivery)
                .where(delivery.userId.eq(userId));

        List<Delivery> deliveries = query
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        Long totalSize = queryFactory.select(delivery.count())
                .from(delivery)
                .where(delivery.userId.eq(userId))
                .fetchOne();

        long totalElements = totalSize != null ? totalSize : 0;
        return new PageImpl<>(deliveries, pageable, totalElements);
    }

    @Override
    public Page<Delivery> getAllDelivery(Pageable pageable, Long userId, String state) {
        JPAQuery<Delivery> query = queryFactory.selectFrom(delivery);

        if (userId != null) {
            query.where(delivery.userId.eq(userId));
        }
        if (state != null && !state.isBlank()) {
            query.where(delivery.state.stringValue().eq(state));
        }

        List<Delivery> deliveries = query
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        Long totalSize = queryFactory.select(delivery.count())
                .from(delivery)
                .where(
                        userId != null ? delivery.userId.eq(userId) : null,
                        state != null && !state.isBlank() ? delivery.state.stringValue().eq(state) : null
                )
                .fetchOne();

        long totalElements = totalSize != null ? totalSize : 0;
        return new PageImpl<>(deliveries, pageable, totalElements);
    }
}
