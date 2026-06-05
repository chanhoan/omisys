package com.omisys.delivery.server.exception;

import com.omisys.common.domain.exception.BusinessException;
import lombok.Getter;

public class DeliveryException extends BusinessException {

    @Getter
    private final DeliveryErrorCode errorCode;

    public DeliveryException(DeliveryErrorCode errorCode, Object... args) {
        super(errorCode.getStatus().name(), errorCode.getMessage(), args);
        this.errorCode = errorCode;
    }
}
