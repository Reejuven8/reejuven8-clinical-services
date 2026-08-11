package com.reejuven8.identity.model.dto;

import jakarta.validation.constraints.NotBlank;

public record AbhaOtpRequest(@NotBlank String mobileNumber) {}
