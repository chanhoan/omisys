package com.omisys.gateway.server.infrastructure.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.omisys.auth.server.auth_dto.jwt.JwtClaim;
import com.omisys.gateway.server.application.UserQueueService;
import com.omisys.gateway.server.application.dto.RegisterUserResponse;
import com.omisys.gateway.server.application.dto.QueueStatusResponse;
import com.omisys.gateway.server.infrastructure.exception.GatewayErrorCode;
import com.omisys.gateway.server.infrastructure.exception.GatewayException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import reactor.core.publisher.Mono;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

import static com.omisys.common.domain.jwt.JwtGlobalConstant.X_USER_CLAIMS;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class GlobalQueueFilterTest {

    private UserQueueService userQueueService;
    private ObjectMapper objectMapper;
    private GlobalQueueFilter filter;

    @BeforeEach
    void setUp() {
        userQueueService = Mockito.mock(UserQueueService.class);
        objectMapper = new ObjectMapper();
        filter = new GlobalQueueFilter(userQueueService, objectMapper, new PublicPathPolicy(false));
    }

    @Test
    void publicPath는_queue를_거치지않고_통과한다() {
        var request = MockServerHttpRequest.get("/api/search").build();
        var exchange = MockServerWebExchange.from(request);

        var chain = new CapturingGatewayFilterChain(null);

        filter.filter(exchange, chain).block();

        assertThat(chain.isCalled()).isTrue();
        verifyNoInteractions(userQueueService);
    }

    @Test
    void protectedPath인데_X_USER_CLAIMS없으면_UNAUTHORIZED예외() {
        var request = MockServerHttpRequest.get("/api/orders").build();
        var exchange = MockServerWebExchange.from(request);

        var chain = new CapturingGatewayFilterChain(null);

        assertThatThrownBy(() -> filter.filter(exchange, chain).block())
                .isInstanceOf(GatewayException.class)
                .satisfies(ex -> {
                    GatewayException ge = (GatewayException) ex;
                    assertThat(ge.getStatusName()).isEqualTo(GatewayErrorCode.UNAUTHORIZED.getStatus().name());
                    assertThat(ge.getMessage()).isEqualTo(GatewayErrorCode.UNAUTHORIZED.getMessage());
                    assertThat(ge.getStatus()).isEqualTo(GatewayErrorCode.UNAUTHORIZED.getStatus());
                });

        assertThat(chain.isCalled()).isFalse();
        verifyNoInteractions(userQueueService);
    }

    @Test
    void isAllowed_true면_chain통과() throws Exception {
        when(userQueueService.isAllowed("1")).thenReturn(Mono.just(true));

        var exchange = exchangeWithClaimsUserId(1L);
        var chain = new CapturingGatewayFilterChain(null);

        filter.filter(exchange, chain).block();

        assertThat(chain.isCalled()).isTrue();
        verify(userQueueService, times(1)).isAllowed("1");
        verify(userQueueService, never()).registerUser(anyString());
    }

    @Test
    void isAllowed_false이고_registerUser_rank0이면_chain통과() throws Exception {
        when(userQueueService.isAllowed("1")).thenReturn(Mono.just(false));
        when(userQueueService.getRetryAfterSeconds()).thenReturn(30L);

        // registerUser 응답 타입은 프로젝트 타입에 맞춰서 바꿔야 함.
        // 아래는 예시: QueueRegisterResponse { long rank; }
        var response = new RegisterUserResponse(0L);
        when(userQueueService.registerUser("1")).thenReturn(Mono.just(response));

        var exchange = exchangeWithClaimsUserId(1L);
        var chain = new CapturingGatewayFilterChain(null);

        filter.filter(exchange, chain).block();

        assertThat(chain.isCalled()).isTrue();
        verify(userQueueService).isAllowed("1");
        verify(userQueueService).registerUser("1");
    }

    @Test
    void isAllowed_false이고_registerUser_rank양수면_200과_X_Queue_Rank를_응답하고_chain미호출() throws Exception {
        when(userQueueService.isAllowed("1")).thenReturn(Mono.just(false));

        when(userQueueService.getRetryAfterSeconds()).thenReturn(30L);
        var response = new RegisterUserResponse(5L);
        when(userQueueService.registerUser("1")).thenReturn(Mono.just(response));

        var exchange = exchangeWithClaimsUserId(1L);
        var chain = new CapturingGatewayFilterChain(null);

        filter.filter(exchange, chain).block();

        assertThat(chain.isCalled()).isFalse();
        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.ACCEPTED);
        assertThat(exchange.getResponse().getHeaders().getFirst("Retry-After")).isEqualTo("30");
        assertThat(exchange.getResponse().getBodyAsString().block())
                .contains("\"state\":\"WAITING\"", "\"rank\":5", "\"retryAfterSeconds\":30");

        verify(userQueueService).isAllowed("1");
        verify(userQueueService).registerUser("1");
    }

    @Test
    void queueStatusReadyReturns200WithoutCallingChain() throws Exception {
        when(userQueueService.getQueueStatus("1")).thenReturn(Mono.just(QueueStatusResponse.ready()));
        var exchange = exchangeWithClaimsUserId(1L, "/api/queue/status");
        var chain = new CapturingGatewayFilterChain(null);

        filter.filter(exchange, chain).block();

        assertThat(chain.isCalled()).isFalse();
        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(exchange.getResponse().getBodyAsString().block()).contains("\"state\":\"READY\"");
        verify(userQueueService).getQueueStatus("1");
        verify(userQueueService, never()).registerUser(anyString());
    }

    @Test
    void queueStatusExpiredReturns410WithoutCallingChain() throws Exception {
        when(userQueueService.getQueueStatus("1")).thenReturn(Mono.just(QueueStatusResponse.expired()));
        var exchange = exchangeWithClaimsUserId(1L, "/api/queue/status");
        var chain = new CapturingGatewayFilterChain(null);

        filter.filter(exchange, chain).block();

        assertThat(chain.isCalled()).isFalse();
        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.GONE);
        assertThat(exchange.getResponse().getBodyAsString().block()).contains("\"state\":\"EXPIRED\"");
    }

    private MockServerWebExchange exchangeWithClaimsUserId(Long userId) throws Exception {
        return exchangeWithClaimsUserId(userId, "/api/orders");
    }

    private MockServerWebExchange exchangeWithClaimsUserId(Long userId, String path) throws Exception {
        JwtClaim claim = new JwtClaim(userId, "u", "ROLE_USER");
        String json = objectMapper.writeValueAsString(claim);
        String encoded = URLEncoder.encode(json, StandardCharsets.UTF_8);

        var request = MockServerHttpRequest.get(path)
                .header(X_USER_CLAIMS, encoded)
                .build();

        return MockServerWebExchange.from(request);
    }

}
