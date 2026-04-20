package com.omisys.review.server.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ReviewErrorCode {
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "내부 서버 오류"),

    REVIEW_NOT_FOUND(HttpStatus.NOT_FOUND, "리뷰를 찾을 수 없습니다."),
    REVIEW_ALREADY_EXISTS(HttpStatus.CONFLICT, "해당 상품에 이미 리뷰를 작성했습니다."),
    REVIEW_FORBIDDEN(HttpStatus.FORBIDDEN, "리뷰 수정/삭제 권한이 없습니다."),
    ORDER_NOT_PURCHASE_CONFIRMED(HttpStatus.BAD_REQUEST, "구매 확정된 주문이 없어 리뷰를 작성할 수 없습니다."),
    ORDER_NOT_VERIFIABLE(HttpStatus.SERVICE_UNAVAILABLE, "주문 확인 서비스에 일시적으로 접근할 수 없습니다.");

    private final HttpStatus status;
    private final String message;
}
