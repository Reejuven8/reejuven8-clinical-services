package com.reejuven8.identity.security;

import com.reejuven8.common.security.JwtClaims;
import com.reejuven8.identity.model.entity.User;
import com.reejuven8.identity.model.enums.UserRole;
import io.jsonwebtoken.JwtException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class JwtTokenProviderTest {

    private static final String SECRET =
        "test-secret-key-at-least-32-characters-long-for-hs256-signing";
    private static final long EXPIRY_MS = 900_000L; // 15 min

    JwtTokenProvider provider;

    @BeforeEach
    void setUp() {
        provider = new JwtTokenProvider(SECRET, EXPIRY_MS);
    }

    private User user(UUID id, UserRole role) {
        return User.builder()
            .id(id)
            .role(role)
            .abhaAddress("test@abdm")
            .build();
    }

    @Test
    void generateToken_thenExtractClaims_roundTrips() {
        UUID userId = UUID.randomUUID();
        String token = provider.generateAccessToken(user(userId, UserRole.PATIENT));

        JwtClaims claims = provider.extractClaims(token);
        assertEquals(userId.toString(), claims.userId());
        assertEquals("PATIENT", claims.role());
        assertEquals("test@abdm", claims.abhaAddress());
    }

    @Test
    void extractRole_matchesInput() {
        String token = provider.generateAccessToken(user(UUID.randomUUID(), UserRole.DOCTOR));
        assertEquals("DOCTOR", provider.extractClaims(token).role());
    }

    @Test
    void tamperedToken_throwsJwtException() {
        String token = provider.generateAccessToken(user(UUID.randomUUID(), UserRole.PATIENT));
        assertThrows(JwtException.class, () -> provider.validateAndExtract(token + "tampered"));
    }

    @Test
    void extractJti_isNonNullAndUniquePerToken() {
        User u = user(UUID.randomUUID(), UserRole.PATIENT);
        String jti1 = provider.extractJti(provider.generateAccessToken(u));
        String jti2 = provider.extractJti(provider.generateAccessToken(u));
        assertNotNull(jti1);
        assertNotEquals(jti1, jti2);
    }

    @Test
    void remainingTtl_isPositiveAndAtMostConfiguredExpiry() {
        String token = provider.generateAccessToken(user(UUID.randomUUID(), UserRole.PATIENT));
        long ttl = provider.getRemainingTtlMs(token);
        assertTrue(ttl > 0 && ttl <= EXPIRY_MS);
    }

    @Test
    void differentUsers_produceDifferentTokens() {
        String t1 = provider.generateAccessToken(user(UUID.randomUUID(), UserRole.PATIENT));
        String t2 = provider.generateAccessToken(user(UUID.randomUUID(), UserRole.PATIENT));
        assertNotEquals(t1, t2);
    }
}
