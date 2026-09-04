package com.omisys.user.exception;

import com.omisys.common.domain.response.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

@Slf4j
@ControllerAdvice
public class UserControllerAdvice {

    @ExceptionHandler(UserException.class)
    public ResponseEntity<?> businessExceptionHandler(UserException e) {
        UserErrorCode errorCode = e.getErrorCode();
        log.error("Error occurs in UserServer : {}", e.getErrorCode());
        return ResponseEntity.status(errorCode.getStatus())
                .body(ApiResponse.error(errorCode.getStatus().name(), errorCode.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> validationExceptionHandler(MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getAllErrors().stream()
                .findFirst()
                .map(error -> error.getDefaultMessage())
                .orElse("입력값이 올바르지 않습니다.");
        return ResponseEntity.badRequest().body(ApiResponse.error("BAD_REQUEST", message));
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<?> runtimeExceptionHandler(RuntimeException e) {
        log.error("Error occurs in UserServer : {}", e.getMessage());
        return ResponseEntity.status(UserErrorCode.INTERNAL_SERVER_ERROR.getStatus())
                .body(ApiResponse.error(UserErrorCode.INTERNAL_SERVER_ERROR.getStatus().name(), e.getMessage()));
    }
}
