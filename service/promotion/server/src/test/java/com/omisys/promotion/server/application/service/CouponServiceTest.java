package com.omisys.promotion.server.application.service;

import com.omisys.common.domain.entity.KafkaTopicConstant;
import com.omisys.promotion.server.domain.model.Coupon;
import com.omisys.promotion.server.domain.model.Event;
import com.omisys.promotion.server.domain.model.UserCoupon;
import com.omisys.promotion.server.domain.model.vo.CouponType;
import com.omisys.promotion.server.domain.model.vo.DiscountType;
import com.omisys.promotion.server.domain.repository.CouponRepository;
import com.omisys.promotion.server.domain.repository.EventRepository;
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
import org.springframework.data.domain.*;
import org.springframework.kafka.core.KafkaTemplate;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CouponServiceTest {

    @Mock private CouponRepository couponRepository;
    @Mock private UserCouponRepository userCouponRepository;
    @Mock private EventRepository eventRepository;
    @Mock private UserService userService;
    @Mock private KafkaTemplate<String, CouponRequest.Event> kafkaTemplate;

    @InjectMocks private CouponService couponService;

    @Test
    @DisplayName("createEventCoupon: eventId가 null이면 EVENT_NOT_FOUND 예외")
    void createEventCoupon_fail_event_id_null() {
        // given
        CouponRequest.Create request = sampleCreateRequest(null);

        // when & then
        assertThatThrownBy(() -> couponService.createEventCoupon(request))
                .isInstanceOf(PromotionException.class)
                .satisfies(ex -> {
                    PromotionException pe = (PromotionException) ex;
                    assertThat(pe.getErrorCode()).isEqualTo(PromotionErrorCode.EVENT_NOT_FOUND);
                });

        verify(eventRepository, never()).findById(anyLong());
        verify(couponRepository, never()).save(any());
    }

    @Test
    @DisplayName("createEventCoupon: 이벤트가 존재하면 Coupon.create(request)로 생성되어 저장된다")
    void createEventCoupon_success_saves_coupon() {
        // given
        Long eventId = 77L;
        CouponRequest.Create request = sampleCreateRequest(eventId);

        // 이벤트 존재 검증 통과
        Event event = mock(Event.class);
        when(eventRepository.findById(eventId)).thenReturn(Optional.of(event));

        // when
        couponService.createEventCoupon(request);

        // then
        // save 호출로 '쿠폰 생성/저장'이 수행되었음을 검증
        verify(couponRepository).save(any(Coupon.class));
    }

    @Test
    @DisplayName("provideEventCouponRequest: Kafka topic(PROVIDE_EVENT_COUPON)로 이벤트 쿠폰 발급 요청을 전송한다")
    void provideEventCouponRequest_sends_kafka_message() {
        // given
        Long userId = 1L;
        Long couponId = 10L;

        // when
        couponService.provideEventCouponRequest(userId, couponId);

        // then
        ArgumentCaptor<CouponRequest.Event> captor = ArgumentCaptor.forClass(CouponRequest.Event.class);
        verify(kafkaTemplate).send(eq(KafkaTopicConstant.PROVIDE_EVENT_COUPON), captor.capture());

        CouponRequest.Event event = captor.getValue();
        assertThat(event.getUserId()).isEqualTo(userId);
        assertThat(event.getCouponId()).isEqualTo(couponId);
    }

    @Test
    @DisplayName("getCouponListBoyUserId: userService 결과가 null이면 INTERNAL_SERVER_ERROR 예외")
    void getCouponListByUserId_fail_user_null() {
        // given
        Long userId = 1L;
        Pageable pageable = PageRequest.of(0, 10);

        when(userService.getUserByUserId(userId)).thenReturn(null);

        // when & then
        assertThatThrownBy(() -> couponService.getCouponListBoyUserId(userId, pageable))
                .isInstanceOf(PromotionException.class)
                .satisfies(ex -> {
                    PromotionException pe = (PromotionException) ex;
                    assertThat(pe.getErrorCode()).isEqualTo(PromotionErrorCode.INTERNAL_SERVER_ERROR);
                });

        verify(userCouponRepository, never()).findByUserId(anyLong(), any());
    }

    @Test
    @DisplayName("getCouponListBoyUserId: userCoupon 목록을 기반으로 coupon을 조회해 응답으로 매핑한다")
    void getCouponListByUserId_success_maps_coupons() {
        // given
        Long userId = 1L;
        Pageable pageable = PageRequest.of(0, 10);

        // 유저 존재 검증 통과
        UserDto userDto = mock(UserDto.class);
        when(userService.getUserByUserId(userId)).thenReturn(userDto);

        // 유저가 가진 쿠폰들
        UserCoupon uc1 = UserCoupon.create(userId, 10L);
        UserCoupon uc2 = UserCoupon.create(userId, 20L);

        Page<UserCoupon> userCoupons = new PageImpl<>(List.of(uc1, uc2), pageable, 2);
        when(userCouponRepository.findByUserId(userId, pageable)).thenReturn(userCoupons);

        // 각 쿠폰 조회
        Coupon c1 = Coupon.create(sampleCreateRequest(777L));
        Coupon c2 = Coupon.create(sampleCreateRequest(777L));

        when(couponRepository.findById(10L)).thenReturn(Optional.of(c1));
        when(couponRepository.findById(20L)).thenReturn(Optional.of(c2));

        // when
        Page<?> result = couponService.getCouponListBoyUserId(userId, pageable);

        // then
        assertThat(result.getTotalElements()).isEqualTo(2);
        verify(couponRepository).findById(10L);
        verify(couponRepository).findById(20L);
    }

    private CouponRequest.Create sampleCreateRequest(Long eventId) {
        Timestamp now = Timestamp.from(Instant.now());
        Timestamp end = Timestamp.from(Instant.now().plusSeconds(3600));

        return new CouponRequest.Create(
                "쿠폰",
                CouponType.EVENT,
                DiscountType.PRICE,
                BigDecimal.valueOf(1000),
                BigDecimal.ZERO,
                BigDecimal.valueOf(1000),
                10,
                now,
                end,
                "BRONZE",
                eventId
        );
    }
}
