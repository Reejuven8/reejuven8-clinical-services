package com.reejuven8.identity.model.dto;
import jakarta.validation.constraints.NotBlank;
public record LoginRequest(@NotBlank String phoneNumber, @NotBlank String otp) {}
