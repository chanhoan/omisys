package com.omisys.notification.server.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum NotificationErrorCode {
    USER_INFO_NOT_FOUND(HttpStatus.NOT_FOUND, "사용자 알림 정보를 찾을 수 없습니다"),
    NOTIFICATION_SEND_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "알림 발송에 실패했습니다"),
    INVALID_ORDER_STATE(HttpStatus.BAD_REQUEST, "알림 대상 주문 상태가 아닙니다");

    private final HttpStatus status;
    private final String message;
}
