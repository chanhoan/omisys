package com.omisys.gateway.server.infrastructure.ratelimit;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.omisys.auth.server.auth_dto.jwt.JwtClaim;
import lombok.RequiredArgsConstructor;
import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

import static com.omisys.common.domain.jwt.JwtGlobalConstant.X_USER_CLAIMS;

@Component("userOrIpKeyResolver")
@RequiredArgsConstructor
public class UserOrIpKeyResolver implements KeyResolver {

    private final ObjectMapper objectMapper;

    @Override
    public Mono<String> resolve(ServerWebExchange exchange) {
        return Mono.just(resolveKey(exchange));
    }

    private String resolveKey(ServerWebExchange exchange) {
        String claimsHeader = exchange.getRequest().getHeaders().getFirst(X_USER_CLAIMS);
        if (claimsHeader != null && !claimsHeader.isBlank()) {
            try {
                String decoded = URLDecoder.decode(claimsHeader, StandardCharsets.UTF_8);
                JwtClaim claim = objectMapper.readValue(decoded, JwtClaim.class);
                if (claim.getUserId() != null) {
                    return "user:" + claim.getUserId();
                }
            } catch (Exception ignored) {
                // Fall back to IP when claims are absent or malformed.
            }
        }

        if (exchange.getRequest().getRemoteAddress() == null) {
            return "ip:unknown";
        }

        return "ip:" + exchange.getRequest().getRemoteAddress().getAddress().getHostAddress();
    }
}
