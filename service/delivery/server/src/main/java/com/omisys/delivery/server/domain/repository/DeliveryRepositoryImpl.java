package com.omisys.delivery.server.domain.repository;

import static com.omisys.delivery.server.domain.model.QDelivery.delivery;

import com.omisys.delivery.server.domain.model.Delivery;
import com.querydsl.core.BooleanBuilder;
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
        List<Delivery> deliveries = queryFactory.selectFrom(delivery)
                .where(delivery.userId.eq(userId))
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
        BooleanBuilder predicate = new BooleanBuilder();
        if (userId != null) predicate.and(delivery.userId.eq(userId));
        if (state != null && !state.isBlank()) predicate.and(delivery.state.stringValue().eq(state));

        List<Delivery> deliveries = queryFactory.selectFrom(delivery)
                .where(predicate)
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        Long totalSize = queryFactory.select(delivery.count())
                .from(delivery)
                .where(predicate)
                .fetchOne();

        long totalElements = totalSize != null ? totalSize : 0;
        return new PageImpl<>(deliveries, pageable, totalElements);
    }
}
