package com.omisys.promotion.server.domain.repository;

import com.omisys.promotion.server.domain.model.Coupon;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;

public interface CouponRepository {

    Coupon save(Coupon coupon);

    Optional<Coupon> findById(Long couponId);

    Optional<Coupon> findByIdWithPessimisticLock(Long couponId);

    Page<Coupon> findAll(Pageable pageable);

    void delete(Coupon coupon);

}
