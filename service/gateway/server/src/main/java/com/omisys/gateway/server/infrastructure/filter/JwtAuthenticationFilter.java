package com.omisys.gateway.server.infrastructure.filter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.omisys.auth.server.auth_dto.jwt.JwtClaim;
import com.omisys.gateway.server.application.AuthService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.context.annotation.Lazy;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

import static com.omisys.common.domain.jwt.JwtGlobalConstant.AUTHORIZATION;
import static com.omisys.common.domain.jwt.JwtGlobalConstant.BEARER_PREFIX;
import static com.omisys.common.domain.jwt.JwtGlobalConstant.X_USER_CLAIMS;

@Slf4j
@Component
@Order(-1)
public class JwtAuthenticationFilter implements GlobalFilter {

    private final AuthService authService;
    private final ObjectMapper objectMapper;

    public JwtAuthenticationFilter(@Lazy AuthService authService, ObjectMapper objectMapper) {
        this.authService = authService;
        this.objectMapper = objectMapper;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String path = exchange.getRequest().getURI().getPath();

        if (isPublicPath(path)) {
            return chain.filter(exchange);
        }

        Optional<String> token = extractToken(exchange);
        if (token.isEmpty()) {
            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
            return exchange.getResponse().setComplete();
        }

        try {
            JwtClaim claims = authService.verifyToken(token.get());

            ServerWebExchange mutatedExchange = addUserClaimsToHeaders(exchange, claims);
            return chain.filter(mutatedExchange);

        } catch (Exception e) {
            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
            return exchange.getResponse().setComplete();
        }
    }

    private boolean isPublicPath(String path) {
        return path.startsWith("/api/auth/")
                || path.startsWith("/api/users/sign-up")
                || path.startsWith("/api/search")
                || path.startsWith("/api/products/search")
                || path.startsWith("/api/preorder/search")
                || path.startsWith("/api/categories/search");
    }

    private ServerWebExchange addUserClaimsToHeaders(ServerWebExchange exchange, JwtClaim claims) {
        if (claims == null) {
            return exchange;
        }

        try {
            String jsonClaims = objectMapper.writeValueAsString(claims);
            String encoded = URLEncoder.encode(jsonClaims, StandardCharsets.UTF_8);

            return exchange.mutate()
                    .request(req -> req.headers(headers -> headers.set(X_USER_CLAIMS, encoded)))
                    .build();

        } catch (JsonProcessingException e) {
            log.error("Error processing JSON: {}", e.getMessage());
            return exchange;
        }
    }

    private Optional<String> extractToken(ServerWebExchange exchange) {
        String header = exchange.getRequest().getHeaders().getFirst(AUTHORIZATION);
        if (header != null && header.startsWith(BEARER_PREFIX)) {
            return Optional.of(header.substring(BEARER_PREFIX.length()));
        }
        return Optional.empty();
    }
}
