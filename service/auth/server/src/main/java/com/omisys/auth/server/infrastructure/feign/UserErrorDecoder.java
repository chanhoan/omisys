package com.omisys.auth.server.infrastructure.feign;

import com.omisys.auth.server.exception.AuthErrorCode;
import com.omisys.auth.server.exception.AuthException;
import feign.Response;
import feign.codec.ErrorDecoder;

public class UserErrorDecoder implements ErrorDecoder {

    @Override
    public Exception decode(String s, Response response) {
        return new AuthException(AuthErrorCode.INTERNAL_SERVER_ERROR);
    }
}
