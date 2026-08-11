package com.reejuven8.ninemo.clinical.model.entity;

import com.reejuven8.ninemo.clinical.model.enums.FoodSafetyRating;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "diet_food_safety")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class DietFoodSafety {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "ingredient_name", nullable = false, unique = true, length = 200)
    private String ingredientName;

    @Column(name = "ingredient_name_hindi", length = 200)
    private String ingredientNameHindi;

    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Enumerated(EnumType.STRING)
    @Column(name = "safety_rating", nullable = false, columnDefinition = "food_safety_rating")
    private FoodSafetyRating safetyRating;

    @Column(name = "medical_reasoning", nullable = false, columnDefinition = "TEXT")
    private String medicalReasoning;

    @Column(name = "safe_quantity", columnDefinition = "TEXT")
    private String safeQuantity;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "trimester_tags", nullable = false, columnDefinition = "jsonb")
    private String trimesterTags = "[1,2,3]";

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "categories", columnDefinition = "jsonb")
    private String categories;

    @Column(name = "is_verified", nullable = false)
    private boolean isVerified = false;

    @Column(name = "verified_by", length = 200)
    private String verifiedBy;

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
