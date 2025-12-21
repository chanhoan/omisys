package com.omisys.slack.server.exception;

import com.omisys.common.domain.response.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
public class MessageExceptionHandler {

    @ExceptionHandler(MessageException.class)
    public ApiResponse<?> handleMessageException(MessageException e) {
        log.error(e.getMessage(), e);
        return ApiResponse.error(e.getStatusName(), e.getMessage());
    }

}
