package com.reejuven8.ninemo.clinical.model.entity;

import com.reejuven8.ninemo.clinical.model.enums.DeliveryType;
import com.reejuven8.ninemo.clinical.model.enums.EddCalculationMethod;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "pregnancy_profiles")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class PregnancyProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "lmp_date")
    private LocalDate lmpDate;

    @Column(name = "ultrasound_date")
    private LocalDate ultrasoundDate;

    @Column(name = "ivf_transfer_date")
    private LocalDate ivfTransferDate;

    @Column(name = "edd_date", nullable = false)
    private LocalDate eddDate;

    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Enumerated(EnumType.STRING)
    @Column(name = "edd_calculation_method", nullable = false,
            columnDefinition = "edd_calculation_method")
    private EddCalculationMethod eddCalculationMethod;

    @Column(name = "height_cm", nullable = false, precision = 5, scale = 2)
    private BigDecimal heightCm;

    @Column(name = "pre_pregnancy_weight_kg", nullable = false, precision = 5, scale = 2)
    private BigDecimal prePregnancyWeightKg;

    @Column(name = "baseline_bmi", nullable = false, precision = 4, scale = 1)
    private BigDecimal baselineBmi;

    @Column(name = "blood_group", nullable = false, length = 5)
    private String bloodGroup;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "high_risk_flags", columnDefinition = "jsonb")
    private String highRiskFlags;

    @Column(name = "is_active", nullable = false)
    private boolean isActive = true;

    @Column(name = "delivery_date")
    private LocalDate deliveryDate;

    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Enumerated(EnumType.STRING)
    @Column(name = "delivery_type", columnDefinition = "delivery_type")
    private DeliveryType deliveryType;

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
