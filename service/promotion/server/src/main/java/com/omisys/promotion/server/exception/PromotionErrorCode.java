package com.omisys.promotion.server.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum PromotionErrorCode {
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "내부 서버 오류"),

    EVENT_NOT_FOUND(HttpStatus.NOT_FOUND, "해당 이벤트를 찾을 수 없습니다"),

    INVALID_COUPON_TYPE(HttpStatus.NOT_FOUND, "해당 쿠폰 종류가 없습니다."),
    INVALID_DISCOUNT_TYPE(HttpStatus.NOT_FOUND, "해당 할인 타입이 없습니다."),
    COUPON_NOT_FOUND(HttpStatus.NOT_FOUND, "해당 쿠폰을 찾을 수 없습니다."),
    INSUFFICIENT_COUPON(HttpStatus.BAD_REQUEST, "쿠폰 수량이 부족합니다."),
    USER_COUPON_NOT_FOUND(HttpStatus.NOT_FOUND, "사용자 쿠폰을 찾을 수 없습니다."),
    COUPON_ALREADY_USED(HttpStatus.BAD_REQUEST, "쿠폰이 이미 사용되었습니다."),
    COUPON_NOT_USED(HttpStatus.BAD_REQUEST, "쿠폰이 사용되지 않았습니다."),
    COUPON_MIN_PRICE_NOT_MET(HttpStatus.BAD_REQUEST, "최소 구매 금액을 충족하지 못했습니다."),
    COUPON_EXPIRED(HttpStatus.BAD_REQUEST, "쿠폰 유효기간이 아닙니다."),
    COUPON_NOT_YET_VALID(HttpStatus.BAD_REQUEST, "쿠폰 사용 가능 기간이 아닙니다.");

    private final HttpStatus status;
    private final String message;
}