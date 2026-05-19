package com.omisys.payment.server.infrastructure.config;

import feign.RequestInterceptor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static com.omisys.common.domain.security.InternalSecretConstant.X_INTERNAL_SECRET;

@Configuration
public class InternalSecretFeignConfig {

    @Bean
    public RequestInterceptor internalSecretRequestInterceptor(
            @Value("${internal.secret:}") String internalSecret) {
        return template -> {
            if (internalSecret != null && !internalSecret.isBlank()) {
                template.header(X_INTERNAL_SECRET, internalSecret);
            }
        };
    }
}
