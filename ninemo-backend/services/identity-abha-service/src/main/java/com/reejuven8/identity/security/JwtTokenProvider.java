package com.reejuven8.identity.security;

import com.reejuven8.common.security.JwtClaims;
import com.reejuven8.identity.model.entity.User;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;

@Component
@Slf4j
public class JwtTokenProvider {

    private final SecretKey key;
    private final long accessTokenExpiryMs;

    public JwtTokenProvider(
        @Value("${jwt.secret}") String secret,
        @Value("${jwt.access-token-expiry-ms}") long accessTokenExpiryMs
    ) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.accessTokenExpiryMs = accessTokenExpiryMs;
    }

    public String generateAccessToken(User user) {
        Instant now = Instant.now();
        return Jwts.builder()
            .id(UUID.randomUUID().toString())
            .subject(user.getId().toString())
            .claim("role", user.getRole().name())
            .claim("abhaAddress", user.getAbhaAddress())
            .issuedAt(Date.from(now))
            .expiration(Date.from(now.plusMillis(accessTokenExpiryMs)))
            .signWith(key)
            .compact();
    }

    public Claims validateAndExtract(String token) {
        return Jwts.parser()
            .verifyWith(key)
            .build()
            .parseSignedClaims(token)
            .getPayload();
    }

    public JwtClaims extractClaims(String token) {
        Claims claims = validateAndExtract(token);
        return new JwtClaims(
            claims.getSubject(),
            claims.get("role", String.class),
            claims.get("abhaAddress", String.class)
        );
    }

    public String extractJti(String token) {
        return validateAndExtract(token).getId();
    }

    public long getRemainingTtlMs(String token) {
        Date expiration = validateAndExtract(token).getExpiration();
        return Math.max(0, expiration.getTime() - System.currentTimeMillis());
    }
}
