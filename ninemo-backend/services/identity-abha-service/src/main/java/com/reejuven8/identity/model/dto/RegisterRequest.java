package com.reejuven8.identity.model.dto;

import com.reejuven8.identity.model.enums.BiologicalSex;
import com.reejuven8.identity.model.enums.UserRole;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

public record RegisterRequest(
    @NotBlank String firstName,
    String middleName,
    @NotBlank String lastName,
    @NotBlank String phoneNumber,
    @NotNull UserRole role,
    @NotNull LocalDate dateOfBirth,
    @NotNull BiologicalSex biologicalSex
) {}
