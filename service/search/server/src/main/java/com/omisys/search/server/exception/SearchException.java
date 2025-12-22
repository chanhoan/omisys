package com.omisys.search.server.exception;

import com.omisys.common.domain.exception.BusinessException;

public class SearchException extends BusinessException {

    public SearchException(SearchErrorCode errorCode) {
        super(errorCode.getStatus().name(), errorCode.getMessage());
    }

}
