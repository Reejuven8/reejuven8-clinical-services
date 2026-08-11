package com.reejuven8.ninemo.clinical.service;

import com.reejuven8.ninemo.clinical.model.document.KickCounterSession;
import com.reejuven8.ninemo.clinical.model.dto.KickSessionResponse;
import com.reejuven8.ninemo.clinical.publisher.ClinicalRiskPublisher;
import com.reejuven8.ninemo.clinical.repository.KickCounterSessionRepository;
import com.reejuven8.ninemo.clinical.repository.PregnancyProfileRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class KickCounterServiceTest {

    @Mock KickCounterSessionRepository sessionRepository;
    @Mock PregnancyProfileRepository pregnancyProfileRepository;
    @Mock ClinicalRiskPublisher riskPublisher;

    KickCounterService service;

    @BeforeEach
    void setUp() {
        service = new KickCounterService(sessionRepository, pregnancyProfileRepository, riskPublisher);
    }

    @Test
    void endSession_lessThan10KicksAfter2Hours_isConcerning() {
        UUID userId = UUID.randomUUID();
        String sessionId = "session-1";
        Instant start = Instant.now().minus(125, ChronoUnit.MINUTES);

        KickCounterSession session = KickCounterSession.builder()
            .id(sessionId)
            .patientId(userId.toString())
            .gestationalWeek(32)
            .totalKicks(7)
            .sessionStart(start)
            .build();

        when(sessionRepository.findById(sessionId)).thenReturn(Optional.of(session));
        when(sessionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        KickSessionResponse response = service.endSession(userId, sessionId);

        assertTrue(response.isConcerning(), "< 10 kicks after 2+ hours should be concerning");
        verify(riskPublisher).publishRiskAlert(eq(userId.toString()), eq("REDUCED_FETAL_MOVEMENT"), eq("CRITICAL"));
    }

    @Test
    void endSession_10OrMoreKicks_notConcerning() {
        UUID userId = UUID.randomUUID();
        String sessionId = "session-2";
        Instant start = Instant.now().minus(130, ChronoUnit.MINUTES);

        KickCounterSession session = KickCounterSession.builder()
            .id(sessionId)
            .patientId(userId.toString())
            .gestationalWeek(30)
            .totalKicks(10)
            .sessionStart(start)
            .build();

        when(sessionRepository.findById(sessionId)).thenReturn(Optional.of(session));
        when(sessionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        KickSessionResponse response = service.endSession(userId, sessionId);

        assertFalse(response.isConcerning(), "10+ kicks should never be concerning");
        verify(riskPublisher, never()).publishRiskAlert(any(), any(), any());
    }

    @Test
    void endSession_lessThan2Hours_notConcerningYet() {
        UUID userId = UUID.randomUUID();
        String sessionId = "session-3";
        Instant start = Instant.now().minus(90, ChronoUnit.MINUTES); // only 90 min

        KickCounterSession session = KickCounterSession.builder()
            .id(sessionId)
            .patientId(userId.toString())
            .gestationalWeek(28)
            .totalKicks(3)
            .sessionStart(start)
            .build();

        when(sessionRepository.findById(sessionId)).thenReturn(Optional.of(session));
        when(sessionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        KickSessionResponse response = service.endSession(userId, sessionId);

        assertFalse(response.isConcerning(), "< 120 minutes should not trigger concern regardless of kicks");
        verify(riskPublisher, never()).publishRiskAlert(any(), any(), any());
    }
}
