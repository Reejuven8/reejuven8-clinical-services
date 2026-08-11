package com.reejuven8.ninemo.clinical.model.dto;

import com.reejuven8.ninemo.clinical.model.enums.DeliveryType;
import com.reejuven8.ninemo.clinical.model.enums.EddCalculationMethod;
import lombok.Builder;
import lombok.Value;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * NM-B-167. gestationalWeek/trimester are server-computed on every read — the client must
 * never derive them (thin-client rule).
 */
@Value
@Builder
public class PregnancyProfileResponse {
    UUID id;
    UUID userId;
    LocalDate lmpDate;
    LocalDate ultrasoundDate;
    LocalDate ivfTransferDate;
    LocalDate eddDate;
    EddCalculationMethod eddCalculationMethod;
    BigDecimal heightCm;
    BigDecimal prePregnancyWeightKg;
    BigDecimal baselineBmi;
    String bloodGroup;
    List<String> highRiskFlags;
    int gestationalWeek;
    int trimester;
    boolean active;
    LocalDate deliveryDate;
    DeliveryType deliveryType;
}
