package com.omisys.review.server.exception;

import com.omisys.common.domain.exception.BusinessException;
import lombok.Getter;

@Getter
public class ReviewException extends BusinessException {

    private final ReviewErrorCode errorCode;

    public ReviewException(ReviewErrorCode errorCode) {
        super(errorCode.getStatus().name(), errorCode.getMessage());
        this.errorCode = errorCode;
    }
}
