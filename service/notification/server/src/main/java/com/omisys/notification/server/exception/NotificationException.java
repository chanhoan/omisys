package com.omisys.notification.server.exception;

import com.omisys.common.domain.exception.BusinessException;
import lombok.Getter;

@Getter
public class NotificationException extends BusinessException {

    private final NotificationErrorCode errorCode;

    public NotificationException(NotificationErrorCode errorCode) {
        super(errorCode.getMessage(), errorCode.name());
        this.errorCode = errorCode;
    }
}
