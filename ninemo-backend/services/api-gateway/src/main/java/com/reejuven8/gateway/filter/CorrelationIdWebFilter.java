package com.reejuven8.gateway.filter;

import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class CorrelationIdWebFilter implements WebFilter {

    static final String CORRELATION_HEADER = "X-Correlation-Id";

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        String cid = exchange.getRequest().getHeaders().getFirst(CORRELATION_HEADER);
        if (cid == null || cid.isBlank()) cid = UUID.randomUUID().toString();

        String finalCid = cid;
        ServerWebExchange mutated = exchange.mutate()
            .request(r -> r.header(CORRELATION_HEADER, finalCid))
            .response(exchange.getResponse())
            .build();

        mutated.getResponse().getHeaders().set(CORRELATION_HEADER, finalCid);
        return chain.filter(mutated);
    }
}
