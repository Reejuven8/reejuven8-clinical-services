package com.reejuven8.ninemo.clinical.service;

import com.reejuven8.common.exception.ResourceNotFoundException;
import com.reejuven8.ninemo.clinical.model.document.ContractionSession;
import com.reejuven8.ninemo.clinical.model.dto.ContractionRequest;
import com.reejuven8.ninemo.clinical.model.dto.ContractionSessionResponse;
import com.reejuven8.ninemo.clinical.publisher.ClinicalRiskPublisher;
import com.reejuven8.ninemo.clinical.repository.ContractionSessionRepository;
import com.reejuven8.ninemo.clinical.repository.PregnancyProfileRepository;
import com.reejuven8.ninemo.clinical.service.timeline.TimelineService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ContractionService {

    // Labor pattern: contractions ≤5 min apart, ≥60s duration, for ≥1 hour
    private static final int LABOR_INTERVAL_THRESHOLD_SECONDS = 300;
    private static final int LABOR_DURATION_THRESHOLD_SECONDS = 60;

    private final ContractionSessionRepository sessionRepository;
    private final PregnancyProfileRepository pregnancyProfileRepository;
    private final ClinicalRiskPublisher riskPublisher;

    public ContractionSessionResponse startSession(UUID userId) {
        var profile = pregnancyProfileRepository.findByUserIdAndIsActiveTrue(userId)
            .orElseThrow(() -> new ResourceNotFoundException("PregnancyProfile", "active profile for user " + userId));

        ContractionSession session = ContractionSession.builder()
            .patientId(userId.toString())
            .pregnancyProfileId(profile.getId().toString())
            .gestationalWeek(TimelineService.computeGestationalWeek(profile.getEddDate()))
            .sessionStart(Instant.now())
            .contractions(new ArrayList<>())
            .totalContractions(0)
            .createdAt(Instant.now())
            .build();

        return toResponse(sessionRepository.save(session));
    }

    public ContractionSessionResponse recordContraction(UUID userId, String sessionId, ContractionRequest request) {
        ContractionSession session = sessionRepository.findById(sessionId)
            .orElseThrow(() -> new IllegalStateException("Session not found: " + sessionId));

        Instant now = Instant.now();
        Map<String, Object> contraction = new HashMap<>();
        contraction.put("startTime", now.toString());
        contraction.put("durationSeconds", request.getDurationSeconds());
        contraction.put("intensity", request.getIntensity());

        List<Map<String, Object>> contractions = session.getContractions();
        if (contractions == null) contractions = new ArrayList<>();

        // Calculate interval from previous contraction
        if (!contractions.isEmpty()) {
            Map<String, Object> prev = contractions.get(contractions.size() - 1);
            Instant prevStart = Instant.parse((String) prev.get("startTime"));
            long intervalSecs = now.getEpochSecond() - prevStart.getEpochSecond();
            contraction.put("intervalFromPreviousSeconds", intervalSecs);
        }

        contractions.add(contraction);
        session.setContractions(contractions);
        session.setTotalContractions(contractions.size());
        session = sessionRepository.save(session);
        return toResponse(session);
    }

    public ContractionSessionResponse endSession(String sessionId) {
        ContractionSession session = sessionRepository.findById(sessionId)
            .orElseThrow(() -> new IllegalStateException("Session not found: " + sessionId));

        session.setSessionEnd(Instant.now());

        List<Map<String, Object>> contractions = session.getContractions();
        if (contractions != null && !contractions.isEmpty()) {
            int avgDuration = (int) contractions.stream()
                .mapToLong(c -> ((Number) c.getOrDefault("durationSeconds", 0)).longValue())
                .average().orElse(0);
            session.setAverageDurationSeconds(avgDuration);

            long[] intervals = contractions.stream()
                .filter(c -> c.containsKey("intervalFromPreviousSeconds"))
                .mapToLong(c -> ((Number) c.get("intervalFromPreviousSeconds")).longValue())
                .toArray();
            int avgInterval = intervals.length > 0
                ? (int) (java.util.Arrays.stream(intervals).average().orElse(0))
                : 0;
            session.setAverageIntervalSeconds(avgInterval);

            boolean laborPattern = avgInterval > 0
                && avgInterval <= LABOR_INTERVAL_THRESHOLD_SECONDS
                && avgDuration >= LABOR_DURATION_THRESHOLD_SECONDS;
            session.setLaborPattern(laborPattern);

            boolean alert = laborPattern && session.getGestationalWeek() < 37;
            session.setAlertTriggered(alert);

            if (alert) {
                riskPublisher.publishRiskAlert(
                    session.getPatientId(), "PREMATURE_LABOR_PATTERN", "CRITICAL"
                );
            }
        }

        return toResponse(sessionRepository.save(session));
    }

    private ContractionSessionResponse toResponse(ContractionSession s) {
        return ContractionSessionResponse.builder()
            .sessionId(s.getId())
            .patientId(s.getPatientId())
            .gestationalWeek(s.getGestationalWeek())
            .sessionStart(s.getSessionStart())
            .sessionEnd(s.getSessionEnd())
            .totalContractions(s.getTotalContractions())
            .averageDurationSeconds(s.getAverageDurationSeconds())
            .averageIntervalSeconds(s.getAverageIntervalSeconds())
            .isLaborPattern(s.isLaborPattern())
            .alertTriggered(s.isAlertTriggered())
            .contractions(s.getContractions())
            .build();
    }
}
