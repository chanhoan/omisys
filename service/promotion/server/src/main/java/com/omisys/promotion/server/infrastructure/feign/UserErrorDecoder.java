package com.omisys.promotion.server.infrastructure.feign;

import com.omisys.promotion.server.exception.PromotionErrorCode;
import com.omisys.promotion.server.exception.PromotionException;
import feign.Response;
import feign.codec.ErrorDecoder;

public class UserErrorDecoder implements ErrorDecoder {

    @Override
    public Exception decode(String methodKey, Response response) {
        return new PromotionException(PromotionErrorCode.INTERNAL_SERVER_ERROR);
    }

}
