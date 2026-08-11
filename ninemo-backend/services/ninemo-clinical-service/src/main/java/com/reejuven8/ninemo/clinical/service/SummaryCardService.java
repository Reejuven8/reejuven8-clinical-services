package com.reejuven8.ninemo.clinical.service;

import com.reejuven8.common.exception.ResourceNotFoundException;
import com.reejuven8.ninemo.clinical.model.document.SymptomLog;
import com.reejuven8.ninemo.clinical.model.document.VitalsLog;
import com.reejuven8.ninemo.clinical.model.dto.SummaryCardResponse;
import com.reejuven8.ninemo.clinical.model.entity.PregnancyProfile;
import com.reejuven8.ninemo.clinical.repository.PregnancyProfileRepository;
import com.reejuven8.ninemo.clinical.repository.SymptomLogRepository;
import com.reejuven8.ninemo.clinical.repository.VitalsLogRepository;
import com.reejuven8.ninemo.clinical.service.timeline.TimelineService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SummaryCardService {

    private final PregnancyProfileRepository pregnancyProfileRepository;
    private final SymptomLogRepository symptomLogRepository;
    private final VitalsLogRepository vitalsLogRepository;
    private final ObjectMapper objectMapper;

    public SummaryCardResponse getSummaryCard(UUID patientId) {
        PregnancyProfile profile = pregnancyProfileRepository.findByUserIdAndIsActiveTrue(patientId)
            .orElseThrow(() -> new ResourceNotFoundException("PregnancyProfile", "active profile for user " + patientId));

        int week = TimelineService.computeGestationalWeek(profile.getEddDate());
        int trimester = TimelineService.computeTrimester(week);

        List<String> highRiskFlags = parseHighRiskFlags(profile.getHighRiskFlags());

        // Latest vitals: last BP and weight entries
        Map<String, Object> latestVitals = Map.of(
            "bp", latestMeasurement(patientId, "bp_systolic"),
            "weight", latestMeasurement(patientId, "weight")
        );

        // Recent symptoms (last 5)
        List<Map<String, Object>> recentSymptoms = symptomLogRepository
            .findTop5ByPatientIdOrderByLoggedAtDesc(patientId.toString())
            .stream()
            .map(log -> Map.<String, Object>of(
                "loggedAt", log.getLoggedAt().toString(),
                "severityFlag", log.getSeverityFlag().name(),
                "symptoms", log.getSymptoms()
            ))
            .collect(Collectors.toList());

        return SummaryCardResponse.builder()
            .patientId(patientId.toString())
            .gestationalWeek(week)
            .trimester(trimester)
            .eddDate(profile.getEddDate())
            .bloodGroup(profile.getBloodGroup())
            .highRiskFlags(highRiskFlags)
            .latestVitals(latestVitals)
            .recentSymptoms(recentSymptoms)
            .pendingMilestones(upcomingMilestones(week))
            .build();
    }

    private Map<String, Object> latestMeasurement(UUID patientId, String vitalType) {
        List<VitalsLog> logs = vitalsLogRepository
            .findTop1ByPatientIdAndVitalTypeOrderByLoggedAtDesc(patientId.toString(), vitalType);
        if (logs.isEmpty()) return Map.of();
        VitalsLog log = logs.get(0);
        return Map.of(
            "value", log.getMeasurements(),
            "loggedAt", log.getLoggedAt().toString(),
            "withinRange", log.isWithinNormalRange()
        );
    }

    @SuppressWarnings("unchecked")
    private List<String> parseHighRiskFlags(String json) {
        if (json == null || json.isBlank()) return List.of();
        try {
            return objectMapper.readValue(json, new TypeReference<List<String>>() {});
        } catch (Exception e) {
            return List.of();
        }
    }

    private List<Map<String, Object>> upcomingMilestones(int week) {
        if (week < 12) return List.of(Map.of("name", "First trimester scan", "dueWeek", 12));
        if (week < 20) return List.of(Map.of("name", "Anomaly scan", "dueWeek", 20));
        if (week < 24) return List.of(Map.of("name", "OGTT (GDM screening)", "dueWeek", 24));
        if (week < 28) return List.of(Map.of("name", "Third trimester begins", "dueWeek", 28));
        if (week < 36) return List.of(Map.of("name", "Group B strep test", "dueWeek", 36));
        return List.of(Map.of("name", "Weekly monitoring", "dueWeek", week + 1));
    }
}
