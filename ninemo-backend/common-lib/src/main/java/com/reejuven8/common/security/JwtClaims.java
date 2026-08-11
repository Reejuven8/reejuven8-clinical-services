package com.reejuven8.common.security;

public record JwtClaims(
    String userId,
    String role,
    String abhaAddress
) {}
