package com.omisys.promotion.server.application.service;

import com.omisys.promotion.server.domain.model.Coupon;
import com.omisys.promotion.server.domain.model.UserCoupon;
import com.omisys.promotion.server.domain.model.vo.DiscountType;
import com.omisys.promotion.server.domain.repository.CouponRepository;
import com.omisys.promotion.server.domain.repository.UserCouponRepository;
import com.omisys.promotion.server.exception.PromotionErrorCode;
import com.omisys.promotion.server.exception.PromotionException;
import com.omisys.user_dto.infrastructure.UserDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;

@Service
@RequiredArgsConstructor
public class CouponInternalService {

    private final UserCouponRepository userCouponRepository;
    private final CouponRepository couponRepository;
    private final UserService userService;

    @Transactional
    public void useCoupon(Long couponId, Long userId) {
        UserCoupon userCoupon = userCouponRepository.findByUserIdAndCouponId(userId, couponId);

        if (userCoupon == null) {
            throw new PromotionException(PromotionErrorCode.USER_COUPON_NOT_FOUND);
        }

        if (userCoupon.getIsUsed()) {
            throw new PromotionException(PromotionErrorCode.COUPON_ALREADY_USED);
        }

        userCoupon.updateIsUsed(true);
    }

    @Transactional
    public void refundCoupon(Long couponId, Long userId) {
        UserCoupon userCoupon = userCouponRepository.findByUserIdAndCouponId(userId, couponId);

        if (userCoupon == null) {
            throw new PromotionException(PromotionErrorCode.USER_COUPON_NOT_FOUND);
        }

        if (!userCoupon.getIsUsed()) {
            throw new PromotionException(PromotionErrorCode.COUPON_NOT_USED);
        }

        userCoupon.updateIsUsed(false);
    }

    @Transactional
    public BigDecimal applyAndGetDiscount(Long couponId, Long userId, BigDecimal productPrice) {
        UserCoupon userCoupon = userCouponRepository.findByUserIdAndCouponId(userId, couponId);

        if (userCoupon == null) {
            throw new PromotionException(PromotionErrorCode.USER_COUPON_NOT_FOUND);
        }
        if (userCoupon.getIsUsed()) {
            throw new PromotionException(PromotionErrorCode.COUPON_ALREADY_USED);
        }

        Coupon coupon = couponRepository.findById(couponId)
                .orElseThrow(() -> new PromotionException(PromotionErrorCode.COUPON_NOT_FOUND));

        Instant now = Instant.now();
        if (coupon.getStartDate().toInstant().isAfter(now)) {
            throw new PromotionException(PromotionErrorCode.COUPON_NOT_YET_VALID);
        }
        if (coupon.getEndDate().toInstant().isBefore(now)) {
            throw new PromotionException(PromotionErrorCode.COUPON_EXPIRED);
        }

        if (coupon.getMinBuyPrice() != null
                && productPrice.compareTo(coupon.getMinBuyPrice()) < 0) {
            throw new PromotionException(PromotionErrorCode.COUPON_MIN_PRICE_NOT_MET);
        }

        BigDecimal discount;
        if (coupon.getDiscountType() == DiscountType.PRICE) {
            discount = coupon.getDiscountValue();
        } else {
            discount = productPrice.multiply(coupon.getDiscountValue())
                    .divide(BigDecimal.valueOf(100), 0, RoundingMode.DOWN);
        }

        if (coupon.getMaxDiscountPrice() != null
                && discount.compareTo(coupon.getMaxDiscountPrice()) > 0) {
            discount = coupon.getMaxDiscountPrice();
        }

        if (discount.compareTo(productPrice) > 0) {
            discount = productPrice;
        }

        userCoupon.updateIsUsed(true);
        return discount;
    }

    @Transactional
    public void provideEventCouponInternal(Long userid, Long couponId) {
        UserDto userData = userService.getUserByUserId(userid);
        if (userData == null) {
            throw new PromotionException(PromotionErrorCode.INTERNAL_SERVER_ERROR);
        }

        Coupon coupon = couponRepository
                .findById(couponId)
                .orElseThrow(() -> new PromotionException(PromotionErrorCode.COUPON_NOT_FOUND));
        if (coupon.getQuantity() - 1 < 0) {
            throw new PromotionException(PromotionErrorCode.INSUFFICIENT_COUPON);
        }

        coupon.updateQuantity(coupon.getQuantity() - 1);
        userCouponRepository.save(UserCoupon.create(userid, couponId));
    }


}
