package com.omisys.gateway.server.infrastructure.filter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.omisys.auth.server.auth_dto.jwt.JwtClaim;
import com.omisys.gateway.server.application.UserQueueService;
import com.omisys.gateway.server.application.dto.QueueState;
import com.omisys.gateway.server.application.dto.QueueStatusResponse;
import com.omisys.gateway.server.infrastructure.exception.GatewayErrorCode;
import com.omisys.gateway.server.infrastructure.exception.GatewayException;
import com.omisys.common.domain.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

import static com.omisys.common.domain.jwt.JwtGlobalConstant.X_USER_CLAIMS;

@Slf4j
@Component
@RequiredArgsConstructor
public class GlobalQueueFilter implements GlobalFilter, Ordered {

    private static final String QUEUE_STATUS_PATH = "/api/queue/status";

    private final UserQueueService userQueueService;
    private final ObjectMapper objectMapper;
    private final PublicPathPolicy publicPathPolicy;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {

        String path = exchange.getRequest().getURI().getPath();

        if (publicPathPolicy.isPublic(path)) {
            return chain.filter(exchange);
        }

        return extractUserId(exchange)
                .flatMap(userId -> isQueueStatusRequest(exchange)
                        ? userQueueService.getQueueStatus(userId)
                        .flatMap(response -> writeQueueResponse(exchange, response))
                        : processRequest(exchange, chain, userId));
    }

    @Override
    public int getOrder() {
        return Ordered.LOWEST_PRECEDENCE;
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
                                return writeQueueResponse(exchange, QueueStatusResponse.waiting(
                                        response.getRank(), userQueueService.getRetryAfterSeconds()));
                            });
                });
    }

    private boolean isQueueStatusRequest(ServerWebExchange exchange) {
        return exchange.getRequest().getMethod() == org.springframework.http.HttpMethod.GET
                && QUEUE_STATUS_PATH.equals(exchange.getRequest().getURI().getPath());
    }

    private Mono<Void> writeQueueResponse(ServerWebExchange exchange, QueueStatusResponse response) {
        HttpStatus status = switch (response.state()) {
            case WAITING -> HttpStatus.ACCEPTED;
            case READY -> HttpStatus.OK;
            case EXPIRED -> HttpStatus.GONE;
        };
        try {
            byte[] body = objectMapper.writeValueAsBytes(new ApiResponse<>(status.name(), null, response));
            var serverResponse = exchange.getResponse();
            HttpHeaders headers = serverResponse.getHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            if (response.state() == QueueState.WAITING) {
                headers.set(HttpHeaders.RETRY_AFTER, String.valueOf(response.retryAfterSeconds()));
            }
            serverResponse.setStatusCode(status);
            DataBuffer buffer = serverResponse.bufferFactory().wrap(body);
            return serverResponse.writeWith(Mono.just(buffer));
        } catch (JsonProcessingException e) {
            return Mono.error(new GatewayException(GatewayErrorCode.INTERNAL_SERVER_ERROR));
        }
    }

}
