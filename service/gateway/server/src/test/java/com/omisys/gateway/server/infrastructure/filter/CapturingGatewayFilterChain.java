package com.omisys.gateway.server.infrastructure.filter;

import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

public class CapturingGatewayFilterChain implements GatewayFilterChain {

    private final AtomicBoolean called = new AtomicBoolean(false);
    private final Consumer<ServerWebExchange> exchangeAsserter;

    public CapturingGatewayFilterChain(Consumer<ServerWebExchange> exchangeAsserter) {
        this.exchangeAsserter = exchangeAsserter;
    }

    public boolean isCalled() {
        return called.get();
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange) {
        called.set(true);
        if (exchangeAsserter != null) {
            exchangeAsserter.accept(exchange);
        }
        return Mono.empty();
    }
}
