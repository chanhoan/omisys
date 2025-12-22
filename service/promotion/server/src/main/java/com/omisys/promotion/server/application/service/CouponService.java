package com.omisys.promotion.server.application.service;

import com.omisys.common.domain.entity.KafkaTopicConstant;
import com.omisys.promotion.server.domain.model.Coupon;
import com.omisys.promotion.server.domain.model.UserCoupon;
import com.omisys.promotion.server.domain.repository.CouponRepository;
import com.omisys.promotion.server.domain.repository.EventRepository;
import com.omisys.promotion.server.domain.repository.UserCouponRepository;
import com.omisys.promotion.server.exception.PromotionErrorCode;
import com.omisys.promotion.server.exception.PromotionException;
import com.omisys.promotion.server.presentation.request.CouponRequest;
import com.omisys.promotion.server.presentation.response.CouponResponse;
import com.omisys.user_dto.infrastructure.UserDto;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CouponService {

    private final CouponRepository couponRepository;
    private final UserCouponRepository userCouponRepository;
    private final EventRepository eventRepository;
    private final UserService userService;
    private final KafkaTemplate<String, CouponRequest.Event> kafkaTemplate;

    @Transactional
    public void createEventCoupon(CouponRequest.Create request) {
        if (request.getEventId() == null) {
            throw new PromotionException(PromotionErrorCode.EVENT_NOT_FOUND);
        }

        eventRepository
                .findById(request.getEventId())
                .orElseThrow(() -> new PromotionException(PromotionErrorCode.EVENT_NOT_FOUND));
        couponRepository.save(Coupon.create(request));
    }

    public void provideEventCouponRequest(Long userId, Long couponId) {
        CouponRequest.Event couponEvent = new CouponRequest.Event(userId, couponId);
        kafkaTemplate.send(KafkaTopicConstant.PROVIDE_EVENT_COUPON, couponEvent);
    }

    public Page<CouponResponse.Get> getCouponList(Pageable pageable) {
        Page<Coupon> coupons = couponRepository.findAll(pageable);
        return coupons.map(CouponResponse.Get::of);
    }

    public CouponResponse.Get getCoupon(Long couponId) {
        Coupon coupon = couponRepository.findById(couponId)
                .orElseThrow(() -> new PromotionException(PromotionErrorCode.COUPON_NOT_FOUND));
        return CouponResponse.Get.of(coupon);
    }

    public Page<CouponResponse.Get> getCouponListBoyUserId(Long userId, Pageable pageable) {
        UserDto userData = userService.getUserByUserId(userId);
        if (userData == null) {
            throw new PromotionException(PromotionErrorCode.INTERNAL_SERVER_ERROR);
        }
        Page<UserCoupon> userCoupons = userCouponRepository.findByUserId(userId, pageable);

        return userCoupons.map(userCoupon -> {
            Coupon coupon = couponRepository.findById(userCoupon.getCouponId())
                    .orElseThrow(() -> new PromotionException(PromotionErrorCode.COUPON_NOT_FOUND));
            return CouponResponse.Get.of(coupon);
        });
    }

    @Transactional
    public void updateCoupon(Long couponId, CouponRequest.Update request) {
        Coupon coupon = couponRepository.findById(couponId)
                .orElseThrow(() -> new PromotionException(PromotionErrorCode.COUPON_NOT_FOUND));
        coupon.update(request);
    }

    @Transactional
    public void deleteCoupon(Long couponId) {
        Coupon coupon = couponRepository.findById(couponId)
                .orElseThrow(() -> new PromotionException(PromotionErrorCode.COUPON_NOT_FOUND));
        couponRepository.delete(coupon);
    }

}
