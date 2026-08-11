package com.reejuven8.ninemo.clinical.service;

import com.reejuven8.ninemo.clinical.model.document.ContractionSession;
import com.reejuven8.ninemo.clinical.model.dto.ContractionSessionResponse;
import com.reejuven8.ninemo.clinical.publisher.ClinicalRiskPublisher;
import com.reejuven8.ninemo.clinical.repository.ContractionSessionRepository;
import com.reejuven8.ninemo.clinical.repository.PregnancyProfileRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ContractionServiceTest {

    @Mock ContractionSessionRepository sessionRepository;
    @Mock PregnancyProfileRepository pregnancyProfileRepository;
    @Mock ClinicalRiskPublisher riskPublisher;

    ContractionService service;

    @BeforeEach
    void setUp() {
        service = new ContractionService(sessionRepository, pregnancyProfileRepository, riskPublisher);
    }

    private ContractionSession sessionWithContractions(int gestationalWeek, int intervalSecs, int durationSecs, int count) {
        List<Map<String, Object>> contractions = new ArrayList<>();
        Instant base = Instant.now().minus(60, ChronoUnit.MINUTES);
        for (int i = 0; i < count; i++) {
            Map<String, Object> c = new java.util.HashMap<>();
            c.put("startTime", base.plusSeconds((long) i * intervalSecs).toString());
            c.put("durationSeconds", durationSecs);
            if (i > 0) c.put("intervalFromPreviousSeconds", (long) intervalSecs);
            contractions.add(c);
        }
        return ContractionSession.builder()
            .id("session-" + gestationalWeek)
            .patientId("patient-1")
            .gestationalWeek(gestationalWeek)
            .sessionStart(base)
            .contractions(contractions)
            .totalContractions(count)
            .createdAt(base)
            .build();
    }

    @Test
    void endSession_laborPattern_before37Weeks_triggersAlert() {
        ContractionSession session = sessionWithContractions(35, 240, 70, 5);
        // interval=240s (≤300), duration=70s (≥60), week=35 (<37) → ALERT
        when(sessionRepository.findById("session-35")).thenReturn(Optional.of(session));
        when(sessionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        ContractionSessionResponse response = service.endSession("session-35");

        assertTrue(response.isLaborPattern(), "Contractions ≤5min apart, ≥60s duration = labor pattern");
        assertTrue(response.isAlertTriggered(), "Labor pattern before 37 weeks should alert");
        verify(riskPublisher).publishRiskAlert(eq("patient-1"), eq("PREMATURE_LABOR_PATTERN"), eq("CRITICAL"));
    }

    @Test
    void endSession_laborPattern_after37Weeks_noAlert() {
        ContractionSession session = sessionWithContractions(38, 240, 70, 5);
        // interval=240s, duration=70s, week=38 (≥37) → labor pattern but no premature alert
        when(sessionRepository.findById("session-38")).thenReturn(Optional.of(session));
        when(sessionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        ContractionSessionResponse response = service.endSession("session-38");

        assertTrue(response.isLaborPattern());
        assertFalse(response.isAlertTriggered(), "At 38 weeks labor pattern is expected — no premature alert");
        verify(riskPublisher, never()).publishRiskAlert(any(), any(), any());
    }

    @Test
    void endSession_wideInterval_noLaborPattern() {
        ContractionSession session = sessionWithContractions(33, 600, 70, 5);
        // interval=600s (>300) → not labor pattern
        when(sessionRepository.findById("session-33")).thenReturn(Optional.of(session));
        when(sessionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        ContractionSessionResponse response = service.endSession("session-33");

        assertFalse(response.isLaborPattern(), "Interval > 5 min should not qualify as labor pattern");
        verify(riskPublisher, never()).publishRiskAlert(any(), any(), any());
    }

    @Test
    void endSession_shortDuration_noLaborPattern() {
        ContractionSession session = sessionWithContractions(34, 240, 45, 5);
        // duration=45s (<60) → not labor pattern
        when(sessionRepository.findById("session-34")).thenReturn(Optional.of(session));
        when(sessionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        ContractionSessionResponse response = service.endSession("session-34");

        assertFalse(response.isLaborPattern(), "Duration < 60s should not qualify as labor pattern");
        verify(riskPublisher, never()).publishRiskAlert(any(), any(), any());
    }
}
