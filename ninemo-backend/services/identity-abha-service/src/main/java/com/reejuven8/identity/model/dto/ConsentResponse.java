package com.reejuven8.identity.model.dto;

import com.reejuven8.identity.model.enums.ConsentStatus;
import java.time.Instant;
import java.util.UUID;

public record ConsentResponse(
    UUID id,
    UUID patientId,
    UUID doctorId,
    ConsentStatus consentStatus,
    Instant grantedAt,
    Instant expiresAt,
    Instant revokedAt
) {}
