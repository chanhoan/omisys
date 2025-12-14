package com.omisys.gateway.server.infrastructure.filter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.omisys.auth.server.auth_dto.jwt.JwtClaim;
import com.omisys.gateway.server.application.UserQueueService;
import com.omisys.gateway.server.infrastructure.exception.GatewayErrorCode;
import com.omisys.gateway.server.infrastructure.exception.GatewayException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

import static com.omisys.common.domain.domain.jwt.JwtGlobalConstant.X_USER_CLAIMS;

@Slf4j
@Component
@RequiredArgsConstructor
public class GlobalQueueFilter implements GlobalFilter, Ordered {

    private final UserQueueService userQueueService;
    private final ObjectMapper objectMapper;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {

        String path = exchange.getRequest().getURI().getPath();

        if (isPublicPath(path)) {
            return chain.filter(exchange);
        }

        return extractUserId(exchange)
                .flatMap(userId -> processRequest(exchange, chain, userId));
    }

    @Override
    public int getOrder() {
        return Ordered.LOWEST_PRECEDENCE;
    }

    private boolean isPublicPath(String path) {
        return path.startsWith("/api/auth/")
                || path.startsWith("/api/users/sign-up")
                || path.startsWith("/api/search")
                || path.startsWith("/api/products/search")
                || path.startsWith("/api/preorder/search")
                || path.startsWith("/api/categories/search");
    }

    private Mono<String> extractUserId(ServerWebExchange exchange) {

        String encodedClaims = exchange.getRequest().getHeaders().getFirst(X_USER_CLAIMS);
        if (encodedClaims == null) {
            return Mono.error(new GatewayException(GatewayErrorCode.UNAUTHORIZED));
        }

        String decodedClaims = URLDecoder.decode(encodedClaims, StandardCharsets.UTF_8);
        try {
            JwtClaim claims = objectMapper.readValue(decodedClaims, JwtClaim.class);
            return Mono.just(claims.getUserId().toString());
        } catch (JsonProcessingException e) {
            return Mono.error(new GatewayException(GatewayErrorCode.BAD_REQUEST));
        }
    }

    private Mono<Void> processRequest(ServerWebExchange exchange, GatewayFilterChain chain,
                                      String userId) {
        return userQueueService.isAllowed(userId)
                .flatMap(allowed -> {
                    if (allowed) {
                        return chain.filter(exchange);
                    }
                    return userQueueService.registerUser(userId)
                            .flatMap(response -> {
                                if (response.getRank() == 0) {
                                    return chain.filter(exchange);
                                }
                                var responseHeaders = exchange.getRequest().getHeaders();
                                responseHeaders.add("X-Queue-Rank", String.valueOf(response.getRank()));
                                exchange.getResponse().setStatusCode(HttpStatus.OK);
                                return exchange.getResponse().setComplete();
                            });
                });
    }

}
