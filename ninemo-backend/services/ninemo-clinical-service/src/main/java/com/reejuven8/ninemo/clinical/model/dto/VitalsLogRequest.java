package com.reejuven8.ninemo.clinical.model.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.Map;

@Data
public class VitalsLogRequest {
    @NotBlank
    private String vitalType;
    private Map<String, Object> measurements;
    private String source;
}
