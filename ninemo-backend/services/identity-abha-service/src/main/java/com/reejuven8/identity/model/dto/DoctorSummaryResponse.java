package com.reejuven8.identity.model.dto;

import java.util.UUID;

public record DoctorSummaryResponse(
    UUID id,
    String firstName,
    String lastName,
    String specialization,
    String qualifications
) {}
