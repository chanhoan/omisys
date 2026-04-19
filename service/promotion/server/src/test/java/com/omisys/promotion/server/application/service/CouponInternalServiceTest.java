package com.omisys.promotion.server.application.service;

import com.omisys.promotion.server.domain.model.Coupon;
import com.omisys.promotion.server.domain.model.UserCoupon;
import com.omisys.promotion.server.domain.model.vo.CouponType;
import com.omisys.promotion.server.domain.model.vo.DiscountType;
import com.omisys.promotion.server.domain.repository.CouponRepository;
import com.omisys.promotion.server.domain.repository.UserCouponRepository;
import com.omisys.promotion.server.exception.PromotionErrorCode;
import com.omisys.promotion.server.exception.PromotionException;
import com.omisys.promotion.server.presentation.request.CouponRequest;
import com.omisys.user_dto.infrastructure.UserDto;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CouponInternalServiceTest {

    @Mock private UserCouponRepository userCouponRepository;
    @Mock private CouponRepository couponRepository;
    @Mock private UserService userService;

    @InjectMocks private CouponInternalService couponInternalService;

    @Test
    @DisplayName("useCoupon: userCoupon이 존재하고 미사용이면 isUsed=true로 전이된다")
    void useCoupon_success_marks_used_true() {
        // given
        Long userId = 1L;
        Long couponId = 10L;

        // UserCoupon은 builder가 private라서 create()로 생성
        UserCoupon userCoupon = UserCoupon.create(userId, couponId);

        when(userCouponRepository.findByUserIdAndCouponId(userId, couponId)).thenReturn(userCoupon);

        // when
        couponInternalService.useCoupon(couponId, userId);

        // then
        // 핵심 검증: 도메인 상태 변화
        assertThat(userCoupon.getIsUsed()).isTrue();
    }

    @Test
    @DisplayName("useCoupon: userCoupon이 없으면 USER_COUPON_NOT_FOUND 예외")
    void useCoupon_fail_user_coupon_not_found() {
        // given
        Long userId = 1L;
        Long couponId = 10L;

        when(userCouponRepository.findByUserIdAndCouponId(userId, couponId)).thenReturn(null);

        // when & then
        assertThatThrownBy(() -> couponInternalService.useCoupon(couponId, userId))
                .isInstanceOf(PromotionException.class)
                .satisfies(ex -> {
                    PromotionException pe = (PromotionException) ex;
                    assertThat(pe.getErrorCode()).isEqualTo(PromotionErrorCode.USER_COUPON_NOT_FOUND);
                });
    }

    @Test
    @DisplayName("useCoupon: 이미 사용된 쿠폰이면 COUPON_ALREADY_USED 예외")
    void useCoupon_fail_already_used() {
        // given
        Long userId = 1L;
        Long couponId = 10L;

        UserCoupon userCoupon = UserCoupon.create(userId, couponId);
        userCoupon.updateIsUsed(true); // 이미 사용됨 상태로 세팅

        when(userCouponRepository.findByUserIdAndCouponId(userId, couponId)).thenReturn(userCoupon);

        // when & then
        assertThatThrownBy(() -> couponInternalService.useCoupon(couponId, userId))
                .isInstanceOf(PromotionException.class)
                .satisfies(ex -> {
                    PromotionException pe = (PromotionException) ex;
                    assertThat(pe.getErrorCode()).isEqualTo(PromotionErrorCode.COUPON_ALREADY_USED);
                });
    }

    @Test
    @DisplayName("refundCoupon: 사용된 쿠폰이면 isUsed=false로 복구된다")
    void refundCoupon_success_marks_used_false() {
        // given
        Long userId = 1L;
        Long couponId = 10L;

        UserCoupon userCoupon = UserCoupon.create(userId, couponId);
        userCoupon.updateIsUsed(true); // 사용됨 상태

        when(userCouponRepository.findByUserIdAndCouponId(userId, couponId)).thenReturn(userCoupon);

        // when
        couponInternalService.refundCoupon(couponId, userId);

        // then
        assertThat(userCoupon.getIsUsed()).isFalse();
    }

    @Test
    @DisplayName("refundCoupon: 사용되지 않은 쿠폰이면 COUPON_NOT_USED 예외")
    void refundCoupon_fail_not_used() {
        // given
        Long userId = 1L;
        Long couponId = 10L;

        UserCoupon userCoupon = UserCoupon.create(userId, couponId);
        // isUsed=false (기본값)

        when(userCouponRepository.findByUserIdAndCouponId(userId, couponId)).thenReturn(userCoupon);

        // when & then
        assertThatThrownBy(() -> couponInternalService.refundCoupon(couponId, userId))
                .isInstanceOf(PromotionException.class)
                .satisfies(ex -> {
                    PromotionException pe = (PromotionException) ex;
                    assertThat(pe.getErrorCode()).isEqualTo(PromotionErrorCode.COUPON_NOT_USED);
                });
    }

    @Test
    @DisplayName("provideEventCouponInternal: 유저 존재 + 쿠폰 수량 충분이면 (수량-1) + UserCoupon 저장이 수행된다")
    void provideEventCouponInternal_success_decrease_quantity_and_save_user_coupon() {
        // given
        Long userId = 1L;
        Long couponId = 10L;

        // 유저 조회 성공해야 함
        UserDto userDto = mock(UserDto.class);
        when(userService.getUserByUserId(userId)).thenReturn(userDto);

        // Coupon은 builder가 private라 create(request)로 생성
        Coupon coupon = Coupon.create(sampleCreateRequestWithQuantity(1, 999L));

        when(couponRepository.findById(couponId)).thenReturn(Optional.of(coupon));

        // when
        couponInternalService.provideEventCouponInternal(userId, couponId);

        // then
        // 핵심: 수량 1 -> 0 으로 감소
        assertThat(coupon.getQuantity()).isEqualTo(0);

        // 핵심: 유저 쿠폰 발급 저장 수행
        verify(userCouponRepository).save(any(UserCoupon.class));
    }

    @Test
    @DisplayName("provideEventCouponInternal: 유저 조회 결과가 null이면 INTERNAL_SERVER_ERROR 예외")
    void provideEventCouponInternal_fail_user_null() {
        // given
        Long userId = 1L;
        Long couponId = 10L;

        when(userService.getUserByUserId(userId)).thenReturn(null);

        // when & then
        assertThatThrownBy(() -> couponInternalService.provideEventCouponInternal(userId, couponId))
                .isInstanceOf(PromotionException.class)
                .satisfies(ex -> {
                    PromotionException pe = (PromotionException) ex;
                    assertThat(pe.getErrorCode()).isEqualTo(PromotionErrorCode.INTERNAL_SERVER_ERROR);
                });

        verify(couponRepository, never()).findById(anyLong());
        verify(userCouponRepository, never()).save(any());
    }

    @Test
    @DisplayName("provideEventCouponInternal: 쿠폰 수량이 0이면 INSUFFICIENT_COUPON 예외")
    void provideEventCouponInternal_fail_insufficient_coupon() {
        // given
        Long userId = 1L;
        Long couponId = 10L;

        UserDto userDto = mock(UserDto.class);
        when(userService.getUserByUserId(userId)).thenReturn(userDto);

        Coupon coupon = Coupon.create(sampleCreateRequestWithQuantity(0, 999L)); // 수량 0

        when(couponRepository.findById(couponId)).thenReturn(Optional.of(coupon));

        // when & then
        assertThatThrownBy(() -> couponInternalService.provideEventCouponInternal(userId, couponId))
                .isInstanceOf(PromotionException.class)
                .satisfies(ex -> {
                    PromotionException pe = (PromotionException) ex;
                    assertThat(pe.getErrorCode()).isEqualTo(PromotionErrorCode.INSUFFICIENT_COUPON);
                });

        // 저장이 일어나면 안 됨
        verify(userCouponRepository, never()).save(any());
    }

    // ── applyAndGetDiscount ─────────────────────────────────────────────────────

    @Test
    @DisplayName("applyAndGetDiscount: PRICE 타입 쿠폰 — 정액 할인 후 isUsed=true")
    void applyAndGetDiscount_price_type_returns_fixed_discount() {
        // given
        Long userId = 1L;
        Long couponId = 10L;
        BigDecimal productPrice = BigDecimal.valueOf(10000);

        UserCoupon userCoupon = UserCoupon.create(userId, couponId);
        when(userCouponRepository.findByUserIdAndCouponId(userId, couponId)).thenReturn(userCoupon);

        Coupon coupon = Coupon.create(priceTypeCouponRequest(BigDecimal.valueOf(2000), null, null));
        when(couponRepository.findById(couponId)).thenReturn(Optional.of(coupon));

        // when
        BigDecimal discount = couponInternalService.applyAndGetDiscount(couponId, userId, productPrice);

        // then
        assertThat(discount).isEqualByComparingTo(BigDecimal.valueOf(2000));
        assertThat(userCoupon.getIsUsed()).isTrue();
    }

    @Test
    @DisplayName("applyAndGetDiscount: PERCENTAGE 타입 쿠폰 — 비율 계산된 할인 반환")
    void applyAndGetDiscount_percentage_type_returns_calculated_discount() {
        // given
        Long userId = 1L;
        Long couponId = 10L;
        BigDecimal productPrice = BigDecimal.valueOf(10000);

        UserCoupon userCoupon = UserCoupon.create(userId, couponId);
        when(userCouponRepository.findByUserIdAndCouponId(userId, couponId)).thenReturn(userCoupon);

        // 10% 할인, 한도 없음
        Coupon coupon = Coupon.create(percentageTypeCouponRequest(BigDecimal.valueOf(10), null, null));
        when(couponRepository.findById(couponId)).thenReturn(Optional.of(coupon));

        // when
        BigDecimal discount = couponInternalService.applyAndGetDiscount(couponId, userId, productPrice);

        // then
        assertThat(discount).isEqualByComparingTo(BigDecimal.valueOf(1000)); // 10000 * 10% = 1000
        assertThat(userCoupon.getIsUsed()).isTrue();
    }

    @Test
    @DisplayName("applyAndGetDiscount: PERCENTAGE 타입 + maxDiscountPrice 설정 — 한도 초과 시 maxDiscountPrice로 캡")
    void applyAndGetDiscount_percentage_type_capped_by_max_discount_price() {
        // given
        Long userId = 1L;
        Long couponId = 10L;
        BigDecimal productPrice = BigDecimal.valueOf(10000);

        UserCoupon userCoupon = UserCoupon.create(userId, couponId);
        when(userCouponRepository.findByUserIdAndCouponId(userId, couponId)).thenReturn(userCoupon);

        // 20% 할인이지만 최대 1500원
        Coupon coupon = Coupon.create(percentageTypeCouponRequest(
                BigDecimal.valueOf(20), null, BigDecimal.valueOf(1500)));
        when(couponRepository.findById(couponId)).thenReturn(Optional.of(coupon));

        // when
        BigDecimal discount = couponInternalService.applyAndGetDiscount(couponId, userId, productPrice);

        // then
        assertThat(discount).isEqualByComparingTo(BigDecimal.valueOf(1500)); // 2000 > 1500 → 캡
        assertThat(userCoupon.getIsUsed()).isTrue();
    }

    @Test
    @DisplayName("applyAndGetDiscount: minBuyPrice 미충족 — COUPON_MIN_PRICE_NOT_MET 예외")
    void applyAndGetDiscount_fail_min_buy_price_not_met() {
        // given
        Long userId = 1L;
        Long couponId = 10L;
        BigDecimal productPrice = BigDecimal.valueOf(3000); // minBuyPrice 5000 미충족

        UserCoupon userCoupon = UserCoupon.create(userId, couponId);
        when(userCouponRepository.findByUserIdAndCouponId(userId, couponId)).thenReturn(userCoupon);

        Coupon coupon = Coupon.create(priceTypeCouponRequest(
                BigDecimal.valueOf(1000), BigDecimal.valueOf(5000), null));
        when(couponRepository.findById(couponId)).thenReturn(Optional.of(coupon));

        // when & then
        assertThatThrownBy(() -> couponInternalService.applyAndGetDiscount(couponId, userId, productPrice))
                .isInstanceOf(PromotionException.class)
                .satisfies(ex -> assertThat(((PromotionException) ex).getErrorCode())
                        .isEqualTo(PromotionErrorCode.COUPON_MIN_PRICE_NOT_MET));

        assertThat(userCoupon.getIsUsed()).isFalse();
    }

    @Test
    @DisplayName("applyAndGetDiscount: 사용자 쿠폰이 없으면 USER_COUPON_NOT_FOUND 예외")
    void applyAndGetDiscount_fail_user_coupon_not_found() {
        // given
        Long userId = 1L;
        Long couponId = 10L;

        when(userCouponRepository.findByUserIdAndCouponId(userId, couponId)).thenReturn(null);

        // when & then
        assertThatThrownBy(() -> couponInternalService.applyAndGetDiscount(couponId, userId, BigDecimal.valueOf(5000)))
                .isInstanceOf(PromotionException.class)
                .satisfies(ex -> assertThat(((PromotionException) ex).getErrorCode())
                        .isEqualTo(PromotionErrorCode.USER_COUPON_NOT_FOUND));
    }

    @Test
    @DisplayName("applyAndGetDiscount: 이미 사용된 쿠폰이면 COUPON_ALREADY_USED 예외")
    void applyAndGetDiscount_fail_already_used() {
        // given
        Long userId = 1L;
        Long couponId = 10L;

        UserCoupon userCoupon = UserCoupon.create(userId, couponId);
        userCoupon.updateIsUsed(true);
        when(userCouponRepository.findByUserIdAndCouponId(userId, couponId)).thenReturn(userCoupon);

        // when & then
        assertThatThrownBy(() -> couponInternalService.applyAndGetDiscount(couponId, userId, BigDecimal.valueOf(5000)))
                .isInstanceOf(PromotionException.class)
                .satisfies(ex -> assertThat(((PromotionException) ex).getErrorCode())
                        .isEqualTo(PromotionErrorCode.COUPON_ALREADY_USED));
    }

    @Test
    @DisplayName("applyAndGetDiscount: PRICE 타입 쿠폰 금액이 상품가 초과 시 상품가로 플로어 처리된다")
    void applyAndGetDiscount_price_type_floored_to_product_price() {
        // given
        Long userId = 1L;
        Long couponId = 10L;
        BigDecimal productPrice = BigDecimal.valueOf(1000); // 상품가보다 큰 쿠폰

        UserCoupon userCoupon = UserCoupon.create(userId, couponId);
        when(userCouponRepository.findByUserIdAndCouponId(userId, couponId)).thenReturn(userCoupon);

        Coupon coupon = Coupon.create(priceTypeCouponRequest(BigDecimal.valueOf(5000), null, null));
        when(couponRepository.findById(couponId)).thenReturn(Optional.of(coupon));

        // when
        BigDecimal discount = couponInternalService.applyAndGetDiscount(couponId, userId, productPrice);

        // then: 할인은 상품가를 초과할 수 없다
        assertThat(discount).isEqualByComparingTo(BigDecimal.valueOf(1000));
        assertThat(userCoupon.getIsUsed()).isTrue();
    }

    @Test
    @DisplayName("applyAndGetDiscount: 쿠폰 시작일 전이면 COUPON_NOT_YET_VALID 예외")
    void applyAndGetDiscount_fail_coupon_not_yet_valid() {
        // given
        Long userId = 1L;
        Long couponId = 10L;

        UserCoupon userCoupon = UserCoupon.create(userId, couponId);
        when(userCouponRepository.findByUserIdAndCouponId(userId, couponId)).thenReturn(userCoupon);

        Coupon coupon = Coupon.create(notYetValidCouponRequest());
        when(couponRepository.findById(couponId)).thenReturn(Optional.of(coupon));

        // when & then
        assertThatThrownBy(() -> couponInternalService.applyAndGetDiscount(couponId, userId, BigDecimal.valueOf(5000)))
                .isInstanceOf(PromotionException.class)
                .satisfies(ex -> assertThat(((PromotionException) ex).getErrorCode())
                        .isEqualTo(PromotionErrorCode.COUPON_NOT_YET_VALID));
    }

    @Test
    @DisplayName("applyAndGetDiscount: 쿠폰 유효기간이 지났으면 COUPON_EXPIRED 예외")
    void applyAndGetDiscount_fail_coupon_expired() {
        // given
        Long userId = 1L;
        Long couponId = 10L;

        UserCoupon userCoupon = UserCoupon.create(userId, couponId);
        when(userCouponRepository.findByUserIdAndCouponId(userId, couponId)).thenReturn(userCoupon);

        Coupon coupon = Coupon.create(expiredCouponRequest());
        when(couponRepository.findById(couponId)).thenReturn(Optional.of(coupon));

        // when & then
        assertThatThrownBy(() -> couponInternalService.applyAndGetDiscount(couponId, userId, BigDecimal.valueOf(5000)))
                .isInstanceOf(PromotionException.class)
                .satisfies(ex -> assertThat(((PromotionException) ex).getErrorCode())
                        .isEqualTo(PromotionErrorCode.COUPON_EXPIRED));
    }

    // ── helpers ─────────────────────────────────────────────────────────────────

    /**
     * Coupon.create(request)가 필요한 필드들을 만족하는 Create 요청을 만들어준다.
     * (promotion 코드상 @NotNull이 붙은 필드는 테스트에서도 항상 채워주는 게 안전함)
     */
    private CouponRequest.Create sampleCreateRequestWithQuantity(int quantity, Long eventId) {
        Timestamp now = Timestamp.from(Instant.now());
        Timestamp end = Timestamp.from(Instant.now().plusSeconds(3600));

        return new CouponRequest.Create(
                "쿠폰",
                CouponType.EVENT,
                DiscountType.PRICE,
                BigDecimal.valueOf(1000),
                BigDecimal.ZERO,
                BigDecimal.valueOf(1000),
                quantity,
                now,
                end,
                "BRONZE",
                eventId
        );
    }

    private CouponRequest.Create priceTypeCouponRequest(
            BigDecimal discountValue, BigDecimal minBuyPrice, BigDecimal maxDiscountPrice) {
        Timestamp now = Timestamp.from(Instant.now());
        Timestamp end = Timestamp.from(Instant.now().plusSeconds(3600));
        return new CouponRequest.Create(
                "정액쿠폰", CouponType.EVENT, DiscountType.PRICE,
                discountValue, minBuyPrice, maxDiscountPrice,
                100, now, end, null, null);
    }

    private CouponRequest.Create percentageTypeCouponRequest(
            BigDecimal discountPercent, BigDecimal minBuyPrice, BigDecimal maxDiscountPrice) {
        Timestamp now = Timestamp.from(Instant.now());
        Timestamp end = Timestamp.from(Instant.now().plusSeconds(3600));
        return new CouponRequest.Create(
                "퍼센트쿠폰", CouponType.EVENT, DiscountType.PERCENTAGE,
                discountPercent, minBuyPrice, maxDiscountPrice,
                100, now, end, null, null);
    }

    private CouponRequest.Create expiredCouponRequest() {
        Timestamp start = Timestamp.from(Instant.now().minusSeconds(7200));
        Timestamp end = Timestamp.from(Instant.now().minusSeconds(3600));
        return new CouponRequest.Create(
                "만료쿠폰", CouponType.EVENT, DiscountType.PRICE,
                BigDecimal.valueOf(1000), null, null,
                100, start, end, null, null);
    }

    private CouponRequest.Create notYetValidCouponRequest() {
        Timestamp start = Timestamp.from(Instant.now().plusSeconds(3600));
        Timestamp end = Timestamp.from(Instant.now().plusSeconds(7200));
        return new CouponRequest.Create(
                "미개시쿠폰", CouponType.EVENT, DiscountType.PRICE,
                BigDecimal.valueOf(1000), null, null,
                100, start, end, null, null);
    }
}
