package com.reejuven8.ninemo.clinical.service;

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
import com.reejuven8.ninemo.clinical.service.timeline.UltrasoundCalculationStrategy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/** NM-B-167 / IS-018. */
@ExtendWith(MockitoExtension.class)
class PregnancyProfileServiceTest {

    @Mock PregnancyProfileRepository repository;

    PregnancyProfileService service;

    private final UUID userId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        service = new PregnancyProfileService(
            repository,
            new LMPCalculationStrategy(),
            new UltrasoundCalculationStrategy(),
            new IVFCalculationStrategy(),
            new ObjectMapper());
    }

    private PregnancyProfileRequest baseRequest() {
        PregnancyProfileRequest r = new PregnancyProfileRequest();
        r.setHeightCm(new BigDecimal("160.00"));
        r.setPrePregnancyWeightKg(new BigDecimal("58.00"));
        r.setBloodGroup("O+");
        r.setHighRiskFlags(List.of("GESTATIONAL_DIABETES_HISTORY"));
        return r;
    }

    private void stubSaveEchoesInput() {
        when(repository.save(any(PregnancyProfile.class))).thenAnswer(inv -> {
            PregnancyProfile p = inv.getArgument(0);
            p.setId(UUID.randomUUID());
            return p;
        });
    }

    @Test
    void create_fromLmp_appliesNaegelesRuleAndComputesBmi() {
        when(repository.findByUserIdAndIsActiveTrue(userId)).thenReturn(Optional.empty());
        stubSaveEchoesInput();

        LocalDate lmp = LocalDate.now().minusDays(70);
        PregnancyProfileRequest request = baseRequest();
        request.setLmpDate(lmp);

        PregnancyProfileResponse response = service.create(userId, request);

        assertEquals(lmp.plusDays(280), response.getEddDate());
        assertEquals(EddCalculationMethod.LMP, response.getEddCalculationMethod());
        // 58 / 1.60² = 22.65625 → 22.7 at DECIMAL(4,1)
        assertEquals(new BigDecimal("22.7"), response.getBaselineBmi());
        assertEquals(List.of("GESTATIONAL_DIABETES_HISTORY"), response.getHighRiskFlags());
        assertTrue(response.isActive());
        assertEquals(11, response.getGestationalWeek()); // 70 days elapsed → week 11
        assertEquals(1, response.getTrimester());
    }

    @Test
    void create_fromIvfTransfer_addsDay5BlastocystOffset() {
        when(repository.findByUserIdAndIsActiveTrue(userId)).thenReturn(Optional.empty());
        stubSaveEchoesInput();

        LocalDate transfer = LocalDate.now().minusDays(30);
        PregnancyProfileRequest request = baseRequest();
        request.setIvfTransferDate(transfer);

        PregnancyProfileResponse response = service.create(userId, request);

        assertEquals(transfer.plusDays(261), response.getEddDate());
        assertEquals(EddCalculationMethod.IVF, response.getEddCalculationMethod());
    }

    @Test
    void create_fromUltrasound_subtractsGestationalAgeAtScan() {
        when(repository.findByUserIdAndIsActiveTrue(userId)).thenReturn(Optional.empty());
        stubSaveEchoesInput();

        LocalDate scan = LocalDate.now().minusDays(20);
        PregnancyProfileRequest request = baseRequest();
        request.setUltrasoundDate(scan);
        request.setUltrasoundGestationalAgeWeeks(12);
        request.setUltrasoundGestationalAgeDays(3);

        PregnancyProfileResponse response = service.create(userId, request);

        assertEquals(scan.plusDays(280 - 87), response.getEddDate());
        assertEquals(EddCalculationMethod.ULTRASOUND, response.getEddCalculationMethod());
    }

    @Test
    void create_ultrasoundWithoutGestationalAge_isRejected() {
        when(repository.findByUserIdAndIsActiveTrue(userId)).thenReturn(Optional.empty());

        PregnancyProfileRequest request = baseRequest();
        request.setUltrasoundDate(LocalDate.now().minusDays(20));

        IllegalArgumentException ex =
            assertThrows(IllegalArgumentException.class, () -> service.create(userId, request));
        assertTrue(ex.getMessage().contains("ultrasoundGestationalAgeWeeks"));
        verify(repository, never()).save(any());
    }

    @Test
    void create_withNoDatingBasis_isRejected() {
        when(repository.findByUserIdAndIsActiveTrue(userId)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> service.create(userId, baseRequest()));
        verify(repository, never()).save(any());
    }

    @Test
    void create_withTwoDatingBases_isRejected() {
        when(repository.findByUserIdAndIsActiveTrue(userId)).thenReturn(Optional.empty());

        PregnancyProfileRequest request = baseRequest();
        request.setLmpDate(LocalDate.now().minusDays(70));
        request.setIvfTransferDate(LocalDate.now().minusDays(30));

        assertThrows(IllegalArgumentException.class, () -> service.create(userId, request));
        verify(repository, never()).save(any());
    }

    @Test
    void create_whenDeclaredMethodContradictsSuppliedDate_isRejected() {
        when(repository.findByUserIdAndIsActiveTrue(userId)).thenReturn(Optional.empty());

        PregnancyProfileRequest request = baseRequest();
        request.setLmpDate(LocalDate.now().minusDays(70));
        request.setEddCalculationMethod(EddCalculationMethod.IVF);

        assertThrows(IllegalArgumentException.class, () -> service.create(userId, request));
    }

    @Test
    void create_futureLmp_isRejected() {
        when(repository.findByUserIdAndIsActiveTrue(userId)).thenReturn(Optional.empty());

        PregnancyProfileRequest request = baseRequest();
        request.setLmpDate(LocalDate.now().plusDays(1));

        assertThrows(IllegalArgumentException.class, () -> service.create(userId, request));
    }

    @Test
    void create_lmpTooLongAgoToBeAPregnancy_isRejected() {
        when(repository.findByUserIdAndIsActiveTrue(userId)).thenReturn(Optional.empty());

        PregnancyProfileRequest request = baseRequest();
        request.setLmpDate(LocalDate.now().minusDays(320)); // EDD already 40 days past

        assertThrows(IllegalArgumentException.class, () -> service.create(userId, request));
    }

    @Test
    void create_whenAnActiveProfileAlreadyExists_conflicts() {
        when(repository.findByUserIdAndIsActiveTrue(userId))
            .thenReturn(Optional.of(PregnancyProfile.builder().id(UUID.randomUUID()).build()));

        PregnancyProfileRequest request = baseRequest();
        request.setLmpDate(LocalDate.now().minusDays(70));

        assertThrows(ConflictException.class, () -> service.create(userId, request));
        verify(repository, never()).save(any());
    }

    @Test
    void getActive_whenOnboardingNotDone_throwsNotFound() {
        when(repository.findByUserIdAndIsActiveTrue(userId)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> service.getActive(userId));
    }

    @Test
    void getActive_returnsProfileWithServerComputedWeek() {
        PregnancyProfile stored = PregnancyProfile.builder()
            .id(UUID.randomUUID())
            .userId(userId)
            .lmpDate(LocalDate.now().minusDays(210))
            .eddDate(LocalDate.now().plusDays(70))
            .eddCalculationMethod(EddCalculationMethod.LMP)
            .heightCm(new BigDecimal("160.00"))
            .prePregnancyWeightKg(new BigDecimal("58.00"))
            .baselineBmi(new BigDecimal("22.7"))
            .bloodGroup("O+")
            .highRiskFlags("[\"ANEMIA\"]")
            .isActive(true)
            .build();
        when(repository.findByUserIdAndIsActiveTrue(userId)).thenReturn(Optional.of(stored));

        PregnancyProfileResponse response = service.getActive(userId);

        assertEquals(31, response.getGestationalWeek());
        assertEquals(3, response.getTrimester());
        assertEquals(List.of("ANEMIA"), response.getHighRiskFlags());
    }
}
