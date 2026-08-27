package com.omisys.payment.server.exception;

import com.omisys.common.domain.exception.BusinessException;

public class PaymentException extends BusinessException {

    public PaymentException(PaymentErrorCode errorCode) {
        super(errorCode.getStatus().name(), errorCode.getMessage());
    }

}
