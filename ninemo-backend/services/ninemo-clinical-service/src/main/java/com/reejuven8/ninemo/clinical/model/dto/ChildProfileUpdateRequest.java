package com.reejuven8.ninemo.clinical.model.dto;

import com.reejuven8.ninemo.clinical.model.enums.BiologicalSex;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * NM-B-169 (3) / IS-029 — partial update. Every field is optional; null means "leave alone",
 * so a PATCH that only renames the baby does not wipe the birth measurements.
 *
 * Also usable as the optional body of POST /mode/transition-to-postnatal, which previously
 * took no body at all and hardcoded childName=null, sex=OTHER, dateOfBirth=now.
 */
@Data
public class ChildProfileUpdateRequest {

    @Size(max = 200)
    private String childName;

    private LocalDate dateOfBirth;

    private BiologicalSex biologicalSex;

    @DecimalMin("0.2") @DecimalMax("12.0")
    private BigDecimal birthWeightKg;

    @DecimalMin("20.0") @DecimalMax("80.0")
    private BigDecimal birthHeightCm;

    @DecimalMin("15.0") @DecimalMax("70.0")
    private BigDecimal headCircumferenceCm;

    @Pattern(regexp = "^(A|B|AB|O)[+-]$", message = "must be one of A+, A-, B+, B-, AB+, AB-, O+, O-")
    private String bloodGroup;
}
