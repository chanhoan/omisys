package com.omisys.gateway.server.infrastructure.exception;

import com.omisys.common.domain.response.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
public class GatewayExceptionHandler {

    /**
     * ApiResponse 만 돌려주면 상태 코드가 200 으로 나간다. 클라이언트가 본문을 파싱하기 전에는
     * 실패를 알 수 없으므로, 오류 코드가 정한 상태를 그대로 실어 보낸다.
     */
    @ExceptionHandler(GatewayException.class)
    public ResponseEntity<ApiResponse<?>> handleGatewayException(GatewayException e) {
        log.error(e.getMessage(), e);
        return ResponseEntity.status(e.getStatus())
                .body(ApiResponse.error(e.getStatusName(), e.getMessage()));
    }
}
