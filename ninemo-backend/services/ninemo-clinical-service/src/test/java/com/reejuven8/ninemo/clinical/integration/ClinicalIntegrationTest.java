package com.reejuven8.ninemo.clinical.integration;

import com.reejuven8.common.exception.ConflictException;
import com.reejuven8.common.exception.ForbiddenException;
import com.reejuven8.ninemo.clinical.model.document.SymptomLog;
import com.reejuven8.ninemo.clinical.model.dto.ChildProfileResponse;
import com.reejuven8.ninemo.clinical.model.dto.ChildProfileUpdateRequest;
import com.reejuven8.ninemo.clinical.model.dto.PregnancyProfileRequest;
import com.reejuven8.ninemo.clinical.model.dto.PregnancyProfileResponse;
import com.reejuven8.ninemo.clinical.model.dto.SymptomLogRequest;
import com.reejuven8.ninemo.clinical.model.dto.SymptomLogResponse;
import com.reejuven8.ninemo.clinical.model.entity.ChildProfile;
import com.reejuven8.ninemo.clinical.model.entity.PregnancyProfile;
import com.reejuven8.ninemo.clinical.model.enums.BiologicalSex;
import com.reejuven8.ninemo.clinical.model.enums.EddCalculationMethod;
import com.reejuven8.ninemo.clinical.model.enums.SeverityFlag;
import com.reejuven8.ninemo.clinical.repository.ChildProfileRepository;
import com.reejuven8.ninemo.clinical.repository.PregnancyProfileRepository;
import com.reejuven8.ninemo.clinical.service.PregnancyProfileService;
import com.reejuven8.ninemo.clinical.service.SymptomService;
import com.reejuven8.ninemo.clinical.service.pediatric.ChildProfileService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Flyway V1–V8 on real PostgreSQL + symptom-log write to real MongoDB.
 * Benign symptoms are used so no RabbitMQ publish is triggered (no broker in this test).
 * Skipped automatically when Docker is not available.
 */
@SpringBootTest
@Testcontainers(disabledWithoutDocker = true)
class ClinicalIntegrationTest {

    @Container
    static final PostgreSQLContainer<?> postgres =
        new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("reejuven8_ninemo");

    @Container
    static final MongoDBContainer mongo = new MongoDBContainer("mongo:7");

    @DynamicPropertySource
    static void props(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.data.mongodb.uri", mongo::getReplicaSetUrl);
    }

    @Autowired PregnancyProfileRepository pregnancyProfileRepository;
    @Autowired ChildProfileRepository childProfileRepository;
    @Autowired SymptomService symptomService;
    @Autowired PregnancyProfileService pregnancyProfileService;
    @Autowired ChildProfileService childProfileService;
    @Autowired JdbcTemplate jdbc;

    @Test
    void flywayMigrationsAppliedIncludingDietSeed() {
        Integer applied = jdbc.queryForObject(
            "SELECT count(*) FROM flyway_schema_history WHERE success = true", Integer.class);
        assertThat(applied).isGreaterThanOrEqualTo(8);
        Integer dietRows = jdbc.queryForObject(
            "SELECT count(*) FROM diet_food_safety", Integer.class);
        assertThat(dietRows).isGreaterThanOrEqualTo(15);
    }

    @Test
    void symptomLogWritesToMongoWithComputedGestationalWeek() {
        UUID userId = UUID.randomUUID();
        pregnancyProfileRepository.save(PregnancyProfile.builder()
            .userId(userId)
            .lmpDate(LocalDate.now().minusDays(210))
            .eddDate(LocalDate.now().plusDays(70))          // ≈ week 31
            .eddCalculationMethod(EddCalculationMethod.LMP)
            .heightCm(new BigDecimal("160.00"))
            .prePregnancyWeightKg(new BigDecimal("58.00"))
            .baselineBmi(new BigDecimal("22.7"))
            .bloodGroup("O+")
            .isActive(true)
            .build());

        SymptomLogRequest request = new SymptomLogRequest();
        request.setSymptoms(List.of(Map.of("name", "Nausea", "severity", "mild")));

        SymptomLogResponse response = symptomService.logSymptoms(userId, request);

        assertThat(response.getId()).isNotNull();
        assertThat(response.getSeverityFlag()).isEqualTo(SeverityFlag.NORMAL);
        assertThat(response.getGestationalWeek()).isBetween(30, 32);

        List<SymptomLog> history = symptomService.getHistory(userId);
        assertThat(history).hasSize(1);
        assertThat(history.get(0).getPatientId()).isEqualTo(userId.toString());
    }

    /**
     * NM-B-167. Exercises the JSONB high_risk_flags column and the edd_calculation_method
     * native PG enum — the two things a mocked repository test cannot cover (see IS-016).
     */
    @Test
    void pregnancyProfileCreateWritesEnumAndJsonbToRealPostgres() {
        UUID userId = UUID.randomUUID();
        PregnancyProfileRequest request = new PregnancyProfileRequest();
        request.setLmpDate(LocalDate.now().minusDays(70));
        request.setHeightCm(new BigDecimal("165.00"));
        request.setPrePregnancyWeightKg(new BigDecimal("60.00"));
        request.setBloodGroup("O+");
        request.setHighRiskFlags(List.of("PREVIOUS_CAESAREAN", "ANEMIA"));

        PregnancyProfileResponse created = pregnancyProfileService.create(userId, request);

        assertThat(created.getId()).isNotNull();
        assertThat(created.getEddCalculationMethod()).isEqualTo(EddCalculationMethod.LMP);
        assertThat(created.getBaselineBmi()).isEqualByComparingTo(new BigDecimal("22.0"));
        assertThat(created.getHighRiskFlags()).containsExactly("PREVIOUS_CAESAREAN", "ANEMIA");
        assertThat(created.getGestationalWeek()).isEqualTo(11);

        PregnancyProfileResponse fetched = pregnancyProfileService.getActive(userId);
        assertThat(fetched.getId()).isEqualTo(created.getId());

        // Second active profile for the same user is rejected, not silently created.
        assertThatThrownBy(() -> pregnancyProfileService.create(userId, request))
            .isInstanceOf(ConflictException.class);
    }

    /** NM-B-169 / IS-027 / IS-028. */
    @Test
    void childProfilesAreListableAndPatchableOnlyByTheirParent() {
        UUID parentId = UUID.randomUUID();
        PregnancyProfile pregnancy = pregnancyProfileRepository.save(PregnancyProfile.builder()
            .userId(parentId)
            .lmpDate(LocalDate.now().minusDays(280))
            .eddDate(LocalDate.now())
            .eddCalculationMethod(EddCalculationMethod.LMP)
            .heightCm(new BigDecimal("160.00"))
            .prePregnancyWeightKg(new BigDecimal("58.00"))
            .baselineBmi(new BigDecimal("22.7"))
            .bloodGroup("A+")
            .isActive(false)
            .build());

        ChildProfile child = childProfileRepository.save(ChildProfile.builder()
            .pregnancyProfileId(pregnancy.getId())
            .parentUserId(parentId)
            .dateOfBirth(LocalDate.now().minusMonths(4))
            .biologicalSex(BiologicalSex.OTHER)
            .isActive(true)
            .build());

        List<ChildProfileResponse> mine = childProfileService.listForParent(parentId);
        assertThat(mine).extracting(ChildProfileResponse::getId).contains(child.getId());
        assertThat(mine.get(0).getAgeInMonths()).isEqualTo(4);

        ChildProfileUpdateRequest patch = new ChildProfileUpdateRequest();
        patch.setChildName("Aarav");
        patch.setBiologicalSex(BiologicalSex.MALE);
        patch.setBirthWeightKg(new BigDecimal("3.10"));

        ChildProfileResponse updated = childProfileService.update(child.getId(), parentId, patch);
        assertThat(updated.getChildName()).isEqualTo("Aarav");
        assertThat(updated.getBiologicalSex()).isEqualTo(BiologicalSex.MALE);
        assertThat(updated.getBirthWeightKg()).isEqualByComparingTo(new BigDecimal("3.10"));
        // Untouched fields survive a partial update.
        assertThat(updated.getPregnancyProfileId()).isEqualTo(pregnancy.getId());

        UUID stranger = UUID.randomUUID();
        assertThat(childProfileService.listForParent(stranger)).isEmpty();
        assertThatThrownBy(() -> childProfileService.get(child.getId(), stranger))
            .isInstanceOf(ForbiddenException.class);
        assertThatThrownBy(() -> childProfileService.update(child.getId(), stranger, patch))
            .isInstanceOf(ForbiddenException.class);
    }
}
