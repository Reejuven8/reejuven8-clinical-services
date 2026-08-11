package com.reejuven8.ninemo.clinical.service;

import com.reejuven8.common.exception.ResourceNotFoundException;
import com.reejuven8.ninemo.clinical.model.document.VitalsLog;
import com.reejuven8.ninemo.clinical.model.dto.VitalsLogRequest;
import com.reejuven8.ninemo.clinical.publisher.ClinicalRiskPublisher;
import com.reejuven8.ninemo.clinical.repository.PregnancyProfileRepository;
import com.reejuven8.ninemo.clinical.repository.VitalsLogRepository;
import com.reejuven8.ninemo.clinical.service.timeline.TimelineService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class VitalsService {

    // Normal ranges per vital type
    private static final Map<String, double[]> NORMAL_RANGES = Map.of(
        "bp_systolic",  new double[]{90, 140},
        "bp_diastolic", new double[]{60, 90},
        "heart_rate",   new double[]{60, 100},
        "fhr",          new double[]{110, 160},
        "temperature",  new double[]{36.1, 37.5},
        "spo2",         new double[]{95, 100}
    );

    private final VitalsLogRepository vitalsLogRepository;
    private final PregnancyProfileRepository pregnancyProfileRepository;
    private final ClinicalRiskPublisher riskPublisher;

    public VitalsLog logVital(UUID userId, VitalsLogRequest request) {
        var profile = pregnancyProfileRepository.findByUserIdAndIsActiveTrue(userId)
            .orElseThrow(() -> new ResourceNotFoundException("PregnancyProfile", "active profile for user " + userId));

        int week = TimelineService.computeGestationalWeek(profile.getEddDate());
        boolean withinRange = isWithinNormalRange(request.getVitalType(), request.getMeasurements());
        boolean alertTriggered = !withinRange;

        VitalsLog log = VitalsLog.builder()
            .patientId(userId.toString())
            .pregnancyProfileId(profile.getId().toString())
            .gestationalWeek(week)
            .trimester(TimelineService.computeTrimester(week))
            .vitalType(request.getVitalType())
            .measurements(request.getMeasurements())
            .source(request.getSource())
            .isWithinNormalRange(withinRange)
            .alertTriggered(alertTriggered)
            .loggedAt(Instant.now())
            .createdAt(Instant.now())
            .build();

        log = vitalsLogRepository.save(log);

        if (alertTriggered) {
            riskPublisher.publishRiskAlert(userId.toString(), "VITALS_OUT_OF_RANGE_" + request.getVitalType().toUpperCase(), "WARNING");
        }

        return log;
    }

    public List<VitalsLog> getVitalsHistory(UUID userId, String vitalType) {
        return vitalsLogRepository.findByPatientIdAndVitalTypeOrderByLoggedAtDesc(
            userId.toString(), vitalType
        );
    }

    private boolean isWithinNormalRange(String vitalType, Map<String, Object> measurements) {
        double[] range = NORMAL_RANGES.get(vitalType.toLowerCase());
        if (range == null || measurements == null) return true;
        Object value = measurements.get("value");
        if (value == null) return true;
        try {
            double v = Double.parseDouble(value.toString());
            return v >= range[0] && v <= range[1];
        } catch (NumberFormatException e) {
            return true;
        }
    }
}
