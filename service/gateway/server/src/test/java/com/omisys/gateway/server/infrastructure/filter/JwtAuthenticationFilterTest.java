package com.omisys.gateway.server.infrastructure.filter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.omisys.auth.server.auth_dto.jwt.JwtClaim;
import com.omisys.gateway.server.application.AuthService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

import static com.omisys.common.domain.jwt.JwtGlobalConstant.AUTHORIZATION;
import static com.omisys.common.domain.jwt.JwtGlobalConstant.BEARER_PREFIX;
import static com.omisys.common.domain.jwt.JwtGlobalConstant.X_USER_CLAIMS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class JwtAuthenticationFilterTest {

    private AuthService authService;
    private ObjectMapper objectMapper;
    private JwtAuthenticationFilter filter;

    @BeforeEach
    void setUp() {
        authService = Mockito.mock(AuthService.class);
        objectMapper = new ObjectMapper();
        filter = new JwtAuthenticationFilter(authService);
    }

    @Test
    void publicPath_auth는_토큰없이도_통과하고_authService를_호출하지않는다() {
        var request = MockServerHttpRequest.get("/api/auth/login").build();
        var exchange = MockServerWebExchange.from(request);

        var chain = new CapturingGatewayFilterChain(ex -> {
            // 체인까지 도달했는지 확인 용도
        });

        filter.filter(exchange, chain).block();

        assertThat(chain.isCalled()).isTrue();
        verifyNoInteractions(authService);
        // public path는 response status를 강제하지 않으므로 null일 수 있음
        assertThat(exchange.getResponse().getStatusCode()).isNull();
    }

    @Test
    void protectedPath_토큰없으면_401이고_chain호출되지않음() {
        var request = MockServerHttpRequest.get("/api/orders").build();
        var exchange = MockServerWebExchange.from(request);

        var chain = new CapturingGatewayFilterChain(null);

        filter.filter(exchange, chain).block();

        assertThat(chain.isCalled()).isFalse();
        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        verifyNoInteractions(authService);
    }

    @Test
    void protectedPath_토큰검증실패면_401이고_chain호출되지않음() {
        when(authService.verifyToken("invalid")).thenThrow(new RuntimeException("boom"));

        var request = MockServerHttpRequest.get("/api/orders")
                .header(AUTHORIZATION, BEARER_PREFIX + "invalid")
                .build();
        var exchange = MockServerWebExchange.from(request);

        var chain = new CapturingGatewayFilterChain(null);

        filter.filter(exchange, chain).block();

        assertThat(chain.isCalled()).isFalse();
        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        verify(authService, times(1)).verifyToken("invalid");
    }

    @Test
    void protectedPath_유효토큰이면_chain호출되고_X_USER_CLAIMS가_추가된다() throws Exception {
        JwtClaim claim = new JwtClaim(1L, "chanhoan", "ROLE_USER");
        when(authService.verifyToken("valid")).thenReturn(claim);

        var request = MockServerHttpRequest.get("/api/orders")
                .header(AUTHORIZATION, BEARER_PREFIX + "valid")
                .build();
        var exchange = MockServerWebExchange.from(request);

        var chain = new CapturingGatewayFilterChain(ex -> {
            String encoded = ex.getRequest().getHeaders().getFirst(X_USER_CLAIMS);
            assertThat(encoded).isNotBlank();

            String decoded = URLDecoder.decode(encoded, StandardCharsets.UTF_8);
            JwtClaim parsed = null;
            try {
                parsed = objectMapper.readValue(decoded, JwtClaim.class);
            } catch (JsonProcessingException e) {
                throw new RuntimeException(e);
            }

            assertThat(parsed.getUserId()).isEqualTo(1L);
            assertThat(parsed.getUsername()).isEqualTo("chanhoan");
            assertThat(parsed.getRole()).isEqualTo("ROLE_USER");
        });

        filter.filter(exchange, chain).block();

        assertThat(chain.isCalled()).isTrue();
        // 성공 케이스에서 응답 status를 직접 세팅하지 않음
        assertThat(exchange.getResponse().getStatusCode()).isNull();

        verify(authService, times(1)).verifyToken("valid");
    }
}
