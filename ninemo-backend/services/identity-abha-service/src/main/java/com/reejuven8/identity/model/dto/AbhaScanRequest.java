package com.reejuven8.identity.model.dto;
import jakarta.validation.constraints.NotBlank;
public record AbhaScanRequest(@NotBlank String encryptedQrPayload, @NotBlank String clientId) {}
