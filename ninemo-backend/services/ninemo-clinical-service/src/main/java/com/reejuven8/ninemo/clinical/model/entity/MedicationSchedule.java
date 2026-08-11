package com.reejuven8.ninemo.clinical.model.entity;

import com.reejuven8.ninemo.clinical.model.enums.MedicationScheduleTime;
import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "medication_schedules")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class MedicationSchedule {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "pregnancy_profile_id")
    private UUID pregnancyProfileId;

    @Column(name = "medication_name", nullable = false, length = 200)
    private String medicationName;

    @Column(name = "dosage", nullable = false, length = 100)
    private String dosage;

    @Column(name = "dosage_instructions", columnDefinition = "TEXT")
    private String dosageInstructions;

    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Enumerated(EnumType.STRING)
    @Column(name = "schedule_time", nullable = false, columnDefinition = "medication_schedule_time")
    private MedicationScheduleTime scheduleTime;

    @Column(name = "reminder_time")
    private LocalTime reminderTime;

    @Column(name = "is_active", nullable = false)
    private boolean isActive = true;

    @Column(name = "current_inventory_count", nullable = false)
    private int currentInventoryCount = 0;

    @Column(name = "refill_threshold", nullable = false)
    private int refillThreshold = 5;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = OffsetDateTime.now();
        updatedAt = OffsetDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = OffsetDateTime.now();
    }
}
