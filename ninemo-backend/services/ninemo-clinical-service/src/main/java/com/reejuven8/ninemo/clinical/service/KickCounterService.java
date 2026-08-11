package com.reejuven8.ninemo.clinical.service;

import com.reejuven8.common.exception.ResourceNotFoundException;
import com.reejuven8.ninemo.clinical.model.document.KickCounterSession;
import com.reejuven8.ninemo.clinical.model.dto.KickSessionResponse;
import com.reejuven8.ninemo.clinical.publisher.ClinicalRiskPublisher;
import com.reejuven8.ninemo.clinical.repository.KickCounterSessionRepository;
import com.reejuven8.ninemo.clinical.repository.PregnancyProfileRepository;
import com.reejuven8.ninemo.clinical.service.timeline.TimelineService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class KickCounterService {

    // Concerning if < 10 kicks in 2 hours (WHO guideline)
    private static final int CONCERN_KICKS_THRESHOLD = 10;
    private static final int CONCERN_MINUTES_THRESHOLD = 120;

    private final KickCounterSessionRepository sessionRepository;
    private final PregnancyProfileRepository pregnancyProfileRepository;
    private final ClinicalRiskPublisher riskPublisher;

    public KickSessionResponse startSession(UUID userId) {
        var profile = pregnancyProfileRepository.findByUserIdAndIsActiveTrue(userId)
            .orElseThrow(() -> new ResourceNotFoundException("PregnancyProfile", "active profile for user " + userId));
        int week = TimelineService.computeGestationalWeek(profile.getEddDate());

        KickCounterSession session = KickCounterSession.builder()
            .patientId(userId.toString())
            .pregnancyProfileId(profile.getId().toString())
            .gestationalWeek(week)
            .sessionStart(Instant.now())
            .totalKicks(0)
            .kickTimestamps(new ArrayList<>())
            .isConcerning(false)
            .createdAt(Instant.now())
            .build();

        session = sessionRepository.save(session);
        return toResponse(session);
    }

    public KickSessionResponse recordKick(UUID userId, String sessionId) {
        KickCounterSession session = sessionRepository.findById(sessionId)
            .orElseThrow(() -> new IllegalStateException("Session not found: " + sessionId));

        Instant now = Instant.now();
        session.getKickTimestamps().add(now);
        session.setTotalKicks(session.getTotalKicks() + 1);

        // Check if 10-kick threshold reached; record time taken
        if (session.getTotalKicks() == CONCERN_KICKS_THRESHOLD && session.getDurationTo10KicksMinutes() == null) {
            long minutes = ChronoUnit.MINUTES.between(session.getSessionStart(), now);
            session.setDurationTo10KicksMinutes((int) minutes);
        }

        session = sessionRepository.save(session);
        return toResponse(session);
    }

    public KickSessionResponse endSession(UUID userId, String sessionId) {
        KickCounterSession session = sessionRepository.findById(sessionId)
            .orElseThrow(() -> new IllegalStateException("Session not found: " + sessionId));

        Instant end = Instant.now();
        session.setSessionEnd(end);

        long totalMinutes = ChronoUnit.MINUTES.between(session.getSessionStart(), end);
        boolean concerning = session.getTotalKicks() < CONCERN_KICKS_THRESHOLD
            && totalMinutes >= CONCERN_MINUTES_THRESHOLD;
        session.setConcerning(concerning);

        session = sessionRepository.save(session);

        if (concerning) {
            riskPublisher.publishRiskAlert(userId.toString(), "REDUCED_FETAL_MOVEMENT", "CRITICAL");
        }

        return toResponse(session);
    }

    private KickSessionResponse toResponse(KickCounterSession s) {
        return KickSessionResponse.builder()
            .sessionId(s.getId())
            .patientId(s.getPatientId())
            .gestationalWeek(s.getGestationalWeek())
            .sessionStart(s.getSessionStart())
            .sessionEnd(s.getSessionEnd())
            .totalKicks(s.getTotalKicks())
            .durationTo10KicksMinutes(s.getDurationTo10KicksMinutes())
            .isConcerning(s.isConcerning())
            .build();
    }
}
