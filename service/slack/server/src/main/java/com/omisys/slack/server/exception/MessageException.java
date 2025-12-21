package com.omisys.slack.server.exception;

import com.omisys.common.domain.exception.BusinessException;

public class MessageException extends BusinessException {

    public MessageException(MessageErrorCode errorCode) {
        super(errorCode.getStatus().name(), errorCode.getMessage());
    }
}