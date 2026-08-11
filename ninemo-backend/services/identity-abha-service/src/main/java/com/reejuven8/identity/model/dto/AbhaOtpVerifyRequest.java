package com.reejuven8.identity.model.dto;

import jakarta.validation.constraints.NotBlank;

public record AbhaOtpVerifyRequest(@NotBlank String txnId, @NotBlank String encryptedOtp) {}
