package com.omisys.gateway.server.infrastructure.ratelimit;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.omisys.auth.server.auth_dto.jwt.JwtClaim;
import org.junit.jupiter.api.Test;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;

import java.net.InetSocketAddress;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

import static com.omisys.common.domain.jwt.JwtGlobalConstant.X_USER_CLAIMS;
import static org.assertj.core.api.Assertions.assertThat;

class UserOrIpKeyResolverTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final UserOrIpKeyResolver resolver = new UserOrIpKeyResolver(objectMapper);

    @Test
    void resolvesUserIdWhenClaimsHeaderExists() throws Exception {
        JwtClaim claim = new JwtClaim(7L, "user", "ROLE_USER");
        String encoded = URLEncoder.encode(objectMapper.writeValueAsString(claim), StandardCharsets.UTF_8);
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/preorder/1")
                        .header(X_USER_CLAIMS, encoded)
                        .remoteAddress(new InetSocketAddress("10.0.0.1", 1234))
                        .build());

        String key = resolver.resolve(exchange).block();

        assertThat(key).isEqualTo("user:7");
    }

    @Test
    void resolvesIpWhenClaimsHeaderMissing() {
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/auth/sign-in")
                        .remoteAddress(new InetSocketAddress("10.0.0.2", 1234))
                        .build());

        String key = resolver.resolve(exchange).block();

        assertThat(key).isEqualTo("ip:10.0.0.2");
    }
}
