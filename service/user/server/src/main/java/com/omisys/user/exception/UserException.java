package com.omisys.user.exception;

import com.omisys.common.domain.domain.exception.BusinessException;
import lombok.Getter;

@Getter
public class UserException extends BusinessException {

    private final UserErrorCode errorCode;

    public UserException(final UserErrorCode errorCode) {
        super(errorCode.getStatus().name(), errorCode.getMessage());
        this.errorCode = errorCode;
    }
}
