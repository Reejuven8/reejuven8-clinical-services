package com.reejuven8.ninemo.clinical.service;

import com.reejuven8.common.exception.ResourceNotFoundException;
import com.reejuven8.ninemo.clinical.model.document.SymptomLog;
import com.reejuven8.ninemo.clinical.model.dto.SymptomLogRequest;
import com.reejuven8.ninemo.clinical.model.dto.SymptomLogResponse;
import com.reejuven8.ninemo.clinical.model.enums.SeverityFlag;
import com.reejuven8.ninemo.clinical.publisher.ClinicalRiskPublisher;
import com.reejuven8.ninemo.clinical.repository.PregnancyProfileRepository;
import com.reejuven8.ninemo.clinical.repository.SymptomLogRepository;
import com.reejuven8.ninemo.clinical.service.timeline.TimelineService;
import com.reejuven8.ninemo.clinical.service.triage.SymptomTriageEngine;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SymptomService {

    private final SymptomLogRepository symptomLogRepository;
    private final PregnancyProfileRepository pregnancyProfileRepository;
    private final SymptomTriageEngine triageEngine;
    private final ClinicalRiskPublisher riskPublisher;

    public SymptomLogResponse logSymptoms(UUID userId, SymptomLogRequest request) {
        var profile = pregnancyProfileRepository.findByUserIdAndIsActiveTrue(userId)
            .orElseThrow(() -> new ResourceNotFoundException("PregnancyProfile", "active profile for user " + userId));

        int week = TimelineService.computeGestationalWeek(profile.getEddDate());
        int trimester = TimelineService.computeTrimester(week);

        SymptomLog log = SymptomLog.builder()
            .patientId(userId.toString())
            .pregnancyProfileId(profile.getId().toString())
            .gestationalWeekAtLog(week)
            .trimester(trimester)
            .symptoms(request.getSymptoms())
            .vitalsAtLog(request.getVitalsAtLog())
            .severityFlag(SeverityFlag.NORMAL)
            .loggedAt(Instant.now())
            .createdAt(Instant.now())
            .build();

        List<String> triggeredRules = triageEngine.evaluate(log);

        log.setTriageResult(Map.of(
            "triggeredRules", triggeredRules,
            "severityFlag", log.getSeverityFlag().name()
        ));

        log = symptomLogRepository.save(log);

        if (log.getSeverityFlag() == SeverityFlag.CRITICAL || log.getSeverityFlag() == SeverityFlag.WARNING) {
            riskPublisher.publishRiskAlert(
                userId.toString(),
                String.join(",", triggeredRules),
                log.getSeverityFlag().name()
            );
        }

        return SymptomLogResponse.builder()
            .id(log.getId())
            .patientId(log.getPatientId())
            .gestationalWeek(week)
            .symptoms(log.getSymptoms())
            .severityFlag(log.getSeverityFlag())
            .triggeredRules(triggeredRules)
            .loggedAt(log.getLoggedAt())
            .build();
    }

    public List<SymptomLog> getHistory(UUID userId) {
        return symptomLogRepository.findByPatientIdOrderByLoggedAtDesc(userId.toString());
    }
}
