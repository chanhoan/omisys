package com.omisys.promotion.server.infrastructure.repository;

import com.omisys.promotion.server.domain.model.Coupon;
import com.omisys.promotion.server.domain.repository.CouponRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class CouponRepositoryImpl implements CouponRepository {

    private final JpaCouponRepository jpaCouponRepository;

    @Override
    public Coupon save(Coupon coupon) {
        return jpaCouponRepository.save(coupon);
    }

    @Override
    public Optional<Coupon> findById(Long couponId) {
        return jpaCouponRepository.findById(couponId);
    }

    @Override
    public Optional<Coupon> findByIdWithPessimisticLock(Long couponId) {
        return jpaCouponRepository.findByIdWithPessimisticLock(couponId);
    }

    @Override
    public Page<Coupon> findAll(Pageable pageable) {
        return jpaCouponRepository.findAll(pageable);
    }

    @Override
    public void delete(Coupon coupon) {
        jpaCouponRepository.delete(coupon);
    }
}
