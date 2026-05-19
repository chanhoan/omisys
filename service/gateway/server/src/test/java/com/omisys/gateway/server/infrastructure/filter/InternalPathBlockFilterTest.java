package com.omisys.gateway.server.infrastructure.filter;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;

import static org.assertj.core.api.Assertions.assertThat;

class InternalPathBlockFilterTest {

    @Test
    void internalPath_isForbidden_beforeRouting() {
        InternalPathBlockFilter filter = new InternalPathBlockFilter();
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/internal/orders").build());
        CapturingGatewayFilterChain chain = new CapturingGatewayFilterChain(null);

        filter.filter(exchange, chain).block();

        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(chain.isCalled()).isFalse();
    }

    @Test
    void apiPath_passes() {
        InternalPathBlockFilter filter = new InternalPathBlockFilter();
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/orders").build());
        CapturingGatewayFilterChain chain = new CapturingGatewayFilterChain(null);

        filter.filter(exchange, chain).block();

        assertThat(exchange.getResponse().getStatusCode()).isNull();
        assertThat(chain.isCalled()).isTrue();
    }
}
