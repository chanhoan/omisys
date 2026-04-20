package com.omisys.review.server.exception;

import com.omisys.common.domain.response.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
public class ReviewControllerAdvice {

    @ExceptionHandler(ReviewException.class)
    public ResponseEntity<?> reviewExceptionHandler(ReviewException e) {
        ReviewErrorCode errorCode = e.getErrorCode();
        log.error("Error occurs in ReviewServer : {}", errorCode);
        return ResponseEntity.status(errorCode.getStatus())
                .body(ApiResponse.error(errorCode.getStatus().name(), errorCode.getMessage()));
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<?> runtimeExceptionHandler(RuntimeException e) {
        log.error("Error occurs in ReviewServer : {}", e.getMessage());
        return ResponseEntity.status(ReviewErrorCode.INTERNAL_SERVER_ERROR.getStatus())
                .body(ApiResponse.error(
                        ReviewErrorCode.INTERNAL_SERVER_ERROR.getStatus().name(), e.getMessage()));
    }
}
