package com.reejuven8.identity.service;

import com.reejuven8.common.exception.UnauthorizedException;
import com.reejuven8.identity.model.dto.TokenResponse;
import com.reejuven8.identity.model.entity.User;
import com.reejuven8.identity.repository.UserRepository;
import com.reejuven8.identity.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TokenService {

    private static final String SESSION_PREFIX = "auth:session:";
    private static final String BLACKLIST_PREFIX = "auth:blacklist:";

    private final JwtTokenProvider jwtTokenProvider;
    private final StringRedisTemplate redis;
    private final UserRepository userRepository;

    @Value("${jwt.refresh-token-expiry-ms}")
    private long refreshExpiryMs;

    public TokenResponse issueTokenPair(User user) {
        String accessToken = jwtTokenProvider.generateAccessToken(user);
        String refreshToken = UUID.randomUUID().toString();
        redis.opsForValue().set(
            SESSION_PREFIX + user.getId(),
            refreshToken,
            Duration.ofMillis(refreshExpiryMs)
        );
        return TokenResponse.of(accessToken, refreshToken);
    }

    public TokenResponse refresh(String refreshToken) {
        // scan all session keys to find match — in production, store reverse index
        String pattern = SESSION_PREFIX + "*";
        var keys = redis.keys(pattern);
        if (keys == null || keys.isEmpty()) {
            throw new UnauthorizedException("Refresh token not found");
        }
        String userId = keys.stream()
            .filter(k -> refreshToken.equals(redis.opsForValue().get(k)))
            .findFirst()
            .map(k -> k.replace(SESSION_PREFIX, ""))
            .orElseThrow(() -> new UnauthorizedException("Refresh token invalid or expired"));

        User user = userRepository.findById(UUID.fromString(userId))
            .orElseThrow(() -> new UnauthorizedException("User not found"));

        // rotate — delete old refresh, issue new pair
        redis.delete(SESSION_PREFIX + userId);
        return issueTokenPair(user);
    }

    public void logout(String accessToken) {
        try {
            String jti = jwtTokenProvider.extractJti(accessToken);
            long ttlMs = jwtTokenProvider.getRemainingTtlMs(accessToken);
            if (ttlMs > 0) {
                redis.opsForValue().set(BLACKLIST_PREFIX + jti, "1", Duration.ofMillis(ttlMs));
            }
        } catch (Exception ignored) {
            // already expired — no blacklist entry needed
        }
    }

    public boolean isBlacklisted(String jti) {
        return Boolean.TRUE.equals(redis.hasKey(BLACKLIST_PREFIX + jti));
    }
}
