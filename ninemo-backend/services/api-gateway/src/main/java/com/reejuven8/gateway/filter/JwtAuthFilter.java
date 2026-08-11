package com.reejuven8.gateway.filter;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.List;

@Component
@Slf4j
public class JwtAuthFilter implements GlobalFilter, Ordered {

    private static final List<String> PUBLIC_PATHS = List.of(
        "/api/v1/identity/auth/",
        "/api/v1/identity/abha/",
        "/api/v1/identity/callback/",
        "/api/v1/notifications/callbacks/",   // vendor webhooks (Twilio) — signature-verified at service
        "/actuator/",
        "/swagger-ui",
        "/v3/api-docs",
        "/ws/"
    );

    private static final String BLACKLIST_PREFIX = "auth:blacklist:";

    private final SecretKey key;
    private final ReactiveStringRedisTemplate redis;

    public JwtAuthFilter(
        @Value("${jwt.secret}") String secret,
        ReactiveStringRedisTemplate redis
    ) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.redis = redis;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String path = exchange.getRequest().getPath().value();

        if (PUBLIC_PATHS.stream().anyMatch(path::startsWith)) {
            return chain.filter(exchange);
        }

        String authHeader = exchange.getRequest().getHeaders().getFirst("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return reject(exchange, HttpStatus.UNAUTHORIZED);
        }

        String token = authHeader.substring(7);
        Claims claims;
        try {
            claims = Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
        } catch (ExpiredJwtException e) {
            return reject(exchange, HttpStatus.UNAUTHORIZED);
        } catch (JwtException e) {
            log.warn("Invalid JWT: {}", e.getMessage());
            return reject(exchange, HttpStatus.UNAUTHORIZED);
        }

        String jti = claims.getId();
        String userId = claims.getSubject();
        String role = claims.get("role", String.class);

        return redis.hasKey(BLACKLIST_PREFIX + jti)
            .flatMap(blacklisted -> {
                if (Boolean.TRUE.equals(blacklisted)) {
                    return reject(exchange, HttpStatus.UNAUTHORIZED);
                }
                ServerHttpRequest mutated = exchange.getRequest().mutate()
                    .header("X-User-Id", userId)
                    .header("X-User-Role", role)
                    .build();
                return chain.filter(exchange.mutate().request(mutated).build());
            });
    }

    private Mono<Void> reject(ServerWebExchange exchange, HttpStatus status) {
        exchange.getResponse().setStatusCode(status);
        return exchange.getResponse().setComplete();
    }

    @Override
    public int getOrder() { return -1; }
}
