package com.reejuven8.identity.model.dto;

import jakarta.validation.constraints.NotBlank;

public record OtpRequest(@NotBlank String phoneNumber) {}
