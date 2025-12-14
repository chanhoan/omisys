package com.omisys.gateway.server.infrastructure.exception;

import com.omisys.common.domain.domain.response.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
public class GatewayExceptionHandler {

    @ExceptionHandler(GatewayException.class)
    public ApiResponse<?> handleGatewayException(GatewayException e) {
        log.error(e.getMessage(), e);
        return ApiResponse.error(e.getStatusName(), e.getMessage());
    }
}
