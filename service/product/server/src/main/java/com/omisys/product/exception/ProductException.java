package com.omisys.product.exception;

import com.omisys.common.domain.domain.exception.BusinessException;
import lombok.Getter;

@Getter
public class ProductException extends BusinessException {
    private final ProductErrorCode errorCode;

    public ProductException(ProductErrorCode errorCode) {
        super(errorCode.getStatus().name(), errorCode.getMessage());
        this.errorCode = errorCode;
    }

    public ProductException(ProductErrorCode errorCode, Object... args) {
        super(errorCode.getStatus().name(), errorCode.getMessage(), args);
        this.errorCode = errorCode;
    }
}
