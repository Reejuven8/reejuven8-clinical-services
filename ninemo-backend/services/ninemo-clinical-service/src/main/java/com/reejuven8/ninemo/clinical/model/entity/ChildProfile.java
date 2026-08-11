package com.reejuven8.ninemo.clinical.model.entity;

import com.reejuven8.ninemo.clinical.model.enums.BiologicalSex;
import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "child_profiles")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ChildProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "pregnancy_profile_id", nullable = false, unique = true)
    private UUID pregnancyProfileId;

    @Column(name = "parent_user_id", nullable = false)
    private UUID parentUserId;

    @Column(name = "child_name", length = 200)
    private String childName;

    @Column(name = "date_of_birth", nullable = false)
    private LocalDate dateOfBirth;

    @Column(name = "birth_weight_kg", precision = 4, scale = 2)
    private BigDecimal birthWeightKg;

    @Column(name = "birth_height_cm", precision = 5, scale = 2)
    private BigDecimal birthHeightCm;

    @Column(name = "head_circumference_cm", precision = 5, scale = 2)
    private BigDecimal headCircumferenceCm;

    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Enumerated(EnumType.STRING)
    @Column(name = "biological_sex", nullable = false, columnDefinition = "biological_sex")
    private BiologicalSex biologicalSex;

    @Column(name = "blood_group", length = 5)
    private String bloodGroup;

    @Column(name = "is_active", nullable = false)
    private boolean isActive = true;

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
