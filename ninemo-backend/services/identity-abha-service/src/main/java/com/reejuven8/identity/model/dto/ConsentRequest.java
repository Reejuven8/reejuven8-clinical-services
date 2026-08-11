package com.reejuven8.identity.model.dto;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
public record ConsentRequest(@NotBlank String doctorId, @NotNull Instant expiresAt) {}
