package com.reejuven8.identity.model.dto;
public record TokenResponse(String accessToken, String refreshToken, String tokenType, long expiresIn) {
    public static TokenResponse of(String access, String refresh) {
        return new TokenResponse(access, refresh, "Bearer", 900L);
    }
}
