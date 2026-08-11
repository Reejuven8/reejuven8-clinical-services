package com.reejuven8.ninemo.clinical.model.dto;

import com.reejuven8.ninemo.clinical.model.enums.EddCalculationMethod;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * NM-B-167 — onboarding payload for POST /api/v1/ninemo/profiles/pregnancy.
 * Field names mirror the pregnancy_profiles columns exactly.
 *
 * Exactly one dating basis must be supplied, and it must match eddCalculationMethod when
 * that is given explicitly (otherwise the method is inferred from whichever date is present).
 * ULTRASOUND additionally requires the gestational age measured at the scan.
 */
@Data
public class PregnancyProfileRequest {

    private EddCalculationMethod eddCalculationMethod;

    private LocalDate lmpDate;

    private LocalDate ultrasoundDate;
    private Integer ultrasoundGestationalAgeWeeks;
    private Integer ultrasoundGestationalAgeDays;

    private LocalDate ivfTransferDate;

    @NotNull
    @DecimalMin("50.0") @DecimalMax("250.0")
    private BigDecimal heightCm;

    @NotNull
    @DecimalMin("20.0") @DecimalMax("300.0")
    private BigDecimal prePregnancyWeightKg;

    @NotNull
    @Pattern(regexp = "^(A|B|AB|O)[+-]$", message = "must be one of A+, A-, B+, B-, AB+, AB-, O+, O-")
    private String bloodGroup;

    /** Free-form obstetric risk markers picked in onboarding step 3; stored as JSONB. */
    @Size(max = 50)
    private List<String> highRiskFlags;
}
