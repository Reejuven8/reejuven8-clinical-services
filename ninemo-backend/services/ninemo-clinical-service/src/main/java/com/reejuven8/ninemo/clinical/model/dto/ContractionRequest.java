package com.reejuven8.ninemo.clinical.model.dto;

import jakarta.validation.constraints.Positive;
import lombok.Data;

@Data
public class ContractionRequest {
    @Positive
    private int durationSeconds;
    private String intensity;
}
