package com.reejuven8.ninemo.clinical.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.reejuven8.common.exception.ConflictException;
import com.reejuven8.common.exception.ResourceNotFoundException;
import com.reejuven8.ninemo.clinical.model.dto.PregnancyProfileRequest;
import com.reejuven8.ninemo.clinical.model.dto.PregnancyProfileResponse;
import com.reejuven8.ninemo.clinical.model.entity.PregnancyProfile;
import com.reejuven8.ninemo.clinical.model.enums.EddCalculationMethod;
import com.reejuven8.ninemo.clinical.repository.PregnancyProfileRepository;
import com.reejuven8.ninemo.clinical.service.timeline.IVFCalculationStrategy;
import com.reejuven8.ninemo.clinical.service.timeline.LMPCalculationStrategy;
import com.reejuven8.ninemo.clinical.service.timeline.TimelineService;
import com.reejuven8.ninemo.clinical.service.timeline.UltrasoundCalculationStrategy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * NM-B-167 / IS-018 — pregnancy profile creation + read.
 *
 * Every other clinical feature (timeline, symptoms, vitals, kick counter, contractions,
 * summary card, mode transition) resolves the caller's active pregnancy_profiles row, so
 * without this service nothing downstream can run end-to-end.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PregnancyProfileService {

    private final PregnancyProfileRepository pregnancyProfileRepository;
    private final LMPCalculationStrategy lmpStrategy;
    private final UltrasoundCalculationStrategy ultrasoundStrategy;
    private final IVFCalculationStrategy ivfStrategy;
    private final ObjectMapper objectMapper;

    private static final int MAX_GESTATION_DAYS = 300; // ~42w6d — beyond this the date is bogus

    @Transactional
    public PregnancyProfileResponse create(UUID userId, PregnancyProfileRequest request) {
        pregnancyProfileRepository.findByUserIdAndIsActiveTrue(userId).ifPresent(existing -> {
            throw new ConflictException(
                "An active pregnancy profile already exists for this user (id=" + existing.getId()
                    + "). Complete the postnatal transition before creating a new one.");
        });

        EddCalculationMethod method = resolveMethod(request);
        LocalDate edd = calculateEdd(method, request);
        validateEddPlausible(edd);

        BigDecimal bmi = computeBmi(request.getHeightCm(), request.getPrePregnancyWeightKg());

        PregnancyProfile profile = PregnancyProfile.builder()
            .userId(userId)
            .lmpDate(request.getLmpDate())
            .ultrasoundDate(request.getUltrasoundDate())
            .ivfTransferDate(request.getIvfTransferDate())
            .eddDate(edd)
            .eddCalculationMethod(method)
            .heightCm(request.getHeightCm())
            .prePregnancyWeightKg(request.getPrePregnancyWeightKg())
            .baselineBmi(bmi)
            .bloodGroup(request.getBloodGroup())
            .highRiskFlags(serializeFlags(request.getHighRiskFlags()))
            .isActive(true)
            .build();

        PregnancyProfile saved = pregnancyProfileRepository.save(profile);
        log.info("Created pregnancyProfileId={} for userId={} via {} (edd={})",
            saved.getId(), userId, method, edd);
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public PregnancyProfileResponse getActive(UUID userId) {
        return toResponse(pregnancyProfileRepository.findByUserIdAndIsActiveTrue(userId)
            .orElseThrow(() -> new ResourceNotFoundException("PregnancyProfile", "active profile for user " + userId)));
    }

    // ── EDD resolution ───────────────────────────────────────────────────────────

    /**
     * Infers the dating method from whichever date was supplied when the client omits it,
     * and rejects ambiguous input (zero or several dates) rather than silently picking one.
     */
    private EddCalculationMethod resolveMethod(PregnancyProfileRequest request) {
        boolean hasLmp = request.getLmpDate() != null;
        boolean hasUltrasound = request.getUltrasoundDate() != null;
        boolean hasIvf = request.getIvfTransferDate() != null;
        int supplied = (hasLmp ? 1 : 0) + (hasUltrasound ? 1 : 0) + (hasIvf ? 1 : 0);

        if (supplied == 0) {
            throw new IllegalArgumentException(
                "Exactly one of lmpDate, ultrasoundDate or ivfTransferDate is required");
        }
        if (supplied > 1) {
            throw new IllegalArgumentException(
                "Exactly one dating basis may be supplied — received " + supplied);
        }

        EddCalculationMethod inferred = hasLmp ? EddCalculationMethod.LMP
            : hasUltrasound ? EddCalculationMethod.ULTRASOUND
            : EddCalculationMethod.IVF;

        EddCalculationMethod declared = request.getEddCalculationMethod();
        if (declared != null && declared != inferred) {
            throw new IllegalArgumentException(
                "eddCalculationMethod=" + declared + " does not match the supplied date (" + inferred + ")");
        }
        return inferred;
    }

    private LocalDate calculateEdd(EddCalculationMethod method, PregnancyProfileRequest request) {
        return switch (method) {
            case LMP -> {
                requireNotFuture(request.getLmpDate(), "lmpDate");
                yield lmpStrategy.calculateEdd(request.getLmpDate());
            }
            case IVF -> {
                requireNotFuture(request.getIvfTransferDate(), "ivfTransferDate");
                yield ivfStrategy.calculateEdd(request.getIvfTransferDate());
            }
            case ULTRASOUND -> {
                requireNotFuture(request.getUltrasoundDate(), "ultrasoundDate");
                if (request.getUltrasoundGestationalAgeWeeks() == null) {
                    throw new IllegalArgumentException(
                        "ultrasoundGestationalAgeWeeks is required when dating by ultrasound");
                }
                int days = request.getUltrasoundGestationalAgeDays() == null
                    ? 0 : request.getUltrasoundGestationalAgeDays();
                yield ultrasoundStrategy.calculateEdd(
                    request.getUltrasoundDate(), request.getUltrasoundGestationalAgeWeeks(), days);
            }
        };
    }

    private void requireNotFuture(LocalDate date, String field) {
        if (date.isAfter(LocalDate.now())) {
            throw new IllegalArgumentException(field + " cannot be in the future");
        }
    }

    private void validateEddPlausible(LocalDate edd) {
        LocalDate today = LocalDate.now();
        if (edd.isBefore(today.minusDays(MAX_GESTATION_DAYS - 280))) {
            throw new IllegalArgumentException(
                "The supplied date yields an EDD of " + edd + ", which is already past term");
        }
        if (edd.isAfter(today.plusDays(MAX_GESTATION_DAYS))) {
            throw new IllegalArgumentException(
                "The supplied date yields an EDD of " + edd + ", which is more than a full gestation away");
        }
    }

    // ── Derived values ───────────────────────────────────────────────────────────

    /** BMI = kg / m², stored at DECIMAL(4,1) precision to match the column. */
    private BigDecimal computeBmi(BigDecimal heightCm, BigDecimal weightKg) {
        BigDecimal heightM = heightCm.divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP);
        return weightKg.divide(heightM.multiply(heightM), 1, RoundingMode.HALF_UP);
    }

    private String serializeFlags(List<String> flags) {
        List<String> safe = flags == null ? List.of() : flags;
        try {
            return objectMapper.writeValueAsString(safe);
        } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
            throw new IllegalArgumentException("highRiskFlags could not be serialised", e);
        }
    }

    private List<String> deserializeFlags(String json) {
        if (json == null || json.isBlank()) return List.of();
        try {
            return objectMapper.readValue(json, new TypeReference<List<String>>() {});
        } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
            log.warn("Unparseable high_risk_flags JSON, returning empty list: {}", json);
            return List.of();
        }
    }

    PregnancyProfileResponse toResponse(PregnancyProfile p) {
        int week = TimelineService.computeGestationalWeek(p.getEddDate());
        return PregnancyProfileResponse.builder()
            .id(p.getId())
            .userId(p.getUserId())
            .lmpDate(p.getLmpDate())
            .ultrasoundDate(p.getUltrasoundDate())
            .ivfTransferDate(p.getIvfTransferDate())
            .eddDate(p.getEddDate())
            .eddCalculationMethod(p.getEddCalculationMethod())
            .heightCm(p.getHeightCm())
            .prePregnancyWeightKg(p.getPrePregnancyWeightKg())
            .baselineBmi(p.getBaselineBmi())
            .bloodGroup(p.getBloodGroup())
            .highRiskFlags(deserializeFlags(p.getHighRiskFlags()))
            .gestationalWeek(week)
            .trimester(TimelineService.computeTrimester(week))
            .active(p.isActive())
            .deliveryDate(p.getDeliveryDate())
            .deliveryType(p.getDeliveryType())
            .build();
    }
}
