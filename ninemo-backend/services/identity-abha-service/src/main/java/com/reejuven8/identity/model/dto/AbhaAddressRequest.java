package com.reejuven8.identity.model.dto;

import jakarta.validation.constraints.NotBlank;

public record AbhaAddressRequest(@NotBlank String txnId, @NotBlank String preferredAddress) {}
