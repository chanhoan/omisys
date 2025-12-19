package com.omisys.payment.server.exception;

import com.omisys.common.domain.response.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
public class PaymentExceptionHandler {

    @ExceptionHandler(PaymentException.class)
    public ApiResponse<?> handleException(PaymentException e) {
        log.error(e.getMessage());
        return ApiResponse.error(e.getStatusName(), e.getMessage());

    }

}