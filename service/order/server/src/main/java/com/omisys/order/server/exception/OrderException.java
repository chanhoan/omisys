package com.omisys.order.server.exception;

import com.omisys.common.domain.exception.BusinessException;
import lombok.Getter;

public class OrderException extends BusinessException {

    @Getter
    private final OrderErrorCode errorCode;

    public OrderException(OrderErrorCode errorCode, Object... args) {
        super(errorCode.getStatus().name(), errorCode.getMessage(), args);
        this.errorCode = errorCode;
    }

}