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
import org.springframework.http.HttpCookie;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.util.MultiValueMap;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;

import static com.omisys.common.domain.jwt.JwtGlobalConstant.AUTHORIZATION;
import static com.omisys.common.domain.jwt.JwtGlobalConstant.BEARER_PREFIX;
import static com.omisys.common.domain.jwt.JwtGlobalConstant.X_USER_CLAIMS;

@Slf4j
@Component
@Order(-1)
public class JwtAuthenticationFilter implements GlobalFilter {

    private static final String ACCESS_TOKEN_COOKIE = "accessToken";

    private static final List<String> PUBLIC_PATHS = List.of(
            "/api/auth/",
            "/api/users/sign-up",
            "/api/search",
            "/api/products/search",
            "/api/preorder/search",
            "/api/categories/search"
    );

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
            log.debug("인증 토큰 없음: path={}", path);
            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
            return exchange.getResponse().setComplete();
        }

        try {
            JwtClaim claims = authService.verifyToken(token.get());
            log.debug("토큰 검증 성공: userId={}", claims.getUserId());
            ServerWebExchange mutated = addUserClaimsToHeaders(exchange, claims);
            return chain.filter(mutated);
        } catch (Exception e) {
            log.warn("토큰 검증 실패: path={}, reason={}", path, e.getMessage());
            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
            return exchange.getResponse().setComplete();
        }
    }

    private boolean isPublicPath(String path) {
        return PUBLIC_PATHS.stream().anyMatch(path::startsWith);
    }

    private Optional<String> extractToken(ServerWebExchange exchange) {
        // 1순위: Cookie에서 accessToken 추출 (브라우저 클라이언트)
        MultiValueMap<String, HttpCookie> cookies = exchange.getRequest().getCookies();
        HttpCookie accessTokenCookie = cookies.getFirst(ACCESS_TOKEN_COOKIE);
        if (accessTokenCookie != null && !accessTokenCookie.getValue().isBlank()) {
            return Optional.of(accessTokenCookie.getValue());
        }

        // 2순위: Authorization Bearer 헤더 (서비스 간 Feign 호출)
        String header = exchange.getRequest().getHeaders().getFirst(AUTHORIZATION);
        if (header != null && header.startsWith(BEARER_PREFIX)) {
            return Optional.of(header.substring(BEARER_PREFIX.length()));
        }

        return Optional.empty();
    }

    private ServerWebExchange addUserClaimsToHeaders(ServerWebExchange exchange, JwtClaim claims) {
        try {
            String jsonClaims = objectMapper.writeValueAsString(claims);
            String encoded = URLEncoder.encode(jsonClaims, StandardCharsets.UTF_8);
            return exchange.mutate()
                    .request(exchange.getRequest().mutate()
                            .header(X_USER_CLAIMS, encoded)
                            .build())
                    .build();
        } catch (JsonProcessingException e) {
            log.error("JwtClaim 직렬화 실패: {}", e.getMessage());
            return exchange;
        }
    }
}
