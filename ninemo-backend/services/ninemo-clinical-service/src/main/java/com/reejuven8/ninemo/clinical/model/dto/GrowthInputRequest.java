package com.reejuven8.ninemo.clinical.model.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class GrowthInputRequest {
    @NotNull @Min(0)
    private Integer ageInMonths;
    @NotNull
    private Double heightCm;
    @NotNull
    private Double weightKg;
    private Double headCircumferenceCm;
}
