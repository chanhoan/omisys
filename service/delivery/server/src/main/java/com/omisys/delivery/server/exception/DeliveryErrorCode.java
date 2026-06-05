package com.omisys.delivery.server.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum DeliveryErrorCode {
    DELIVERY_NOT_FOUND(HttpStatus.NOT_FOUND, "존재하지 않는 배송입니다."),
    DELIVERY_PERMISSION_DENIED(HttpStatus.FORBIDDEN, "해당 배송에 대한 권한이 없습니다."),
    INVALID_DELIVERY_STATE_TRANSITION(HttpStatus.BAD_REQUEST, "허용되지 않는 배송 상태 전이입니다. : [%s]"),
    DELIVERY_ALREADY_EXISTS(HttpStatus.CONFLICT, "이미 배송이 생성된 주문입니다. : [%s]"),
    SERVICE_UNAVAILABLE(HttpStatus.SERVICE_UNAVAILABLE, "일시적으로 서비스를 이용할 수 없습니다. 잠시 후 다시 시도해주세요."),
    ;

    private final HttpStatus status;
    private final String message;

    DeliveryErrorCode(HttpStatus status, String message) {
        this.status = status;
        this.message = message;
    }
}
