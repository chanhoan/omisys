package com.omisys.gateway.server.infrastructure.exception;

import com.omisys.common.domain.domain.exception.BusinessException;

public class GatewayException extends BusinessException {

    public GatewayException(GatewayErrorCode errorCode) {
        super(errorCode.getStatus().name(), errorCode.getMessage());
    }
}
