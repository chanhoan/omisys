package com.omisys.gateway.server.infrastructure.exception;

import com.omisys.common.domain.exception.BusinessException;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class GatewayException extends BusinessException {

    private final HttpStatus status;

    public GatewayException(GatewayErrorCode errorCode) {
        // BusinessException 의 2-인자 생성자는 (message, statusName) 순서다.
        // (statusName, message) 로 넘기면 컴파일은 되지만 두 값이 뒤바뀐 채 응답에 나간다.
        super(errorCode.getMessage(), errorCode.getStatus().name());
        this.status = errorCode.getStatus();
    }
}
