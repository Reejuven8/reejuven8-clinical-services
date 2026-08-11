package com.reejuven8.gateway.filter;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import java.time.Instant;

@Component
@Slf4j
public class RequestLoggingFilter implements GlobalFilter, Ordered {

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        long start = Instant.now().toEpochMilli();
        String method = exchange.getRequest().getMethod().name();
        String path = exchange.getRequest().getPath().value();

        return chain.filter(exchange).then(Mono.fromRunnable(() -> {
            long duration = Instant.now().toEpochMilli() - start;
            int status = exchange.getResponse().getStatusCode() != null
                ? exchange.getResponse().getStatusCode().value() : 0;
            log.info("method={} path={} status={} duration_ms={}", method, path, status, duration);
        }));
    }

    @Override
    public int getOrder() { return Ordered.LOWEST_PRECEDENCE; }
}
