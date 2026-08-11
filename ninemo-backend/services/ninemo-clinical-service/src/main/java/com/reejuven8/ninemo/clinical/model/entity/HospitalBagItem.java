package com.reejuven8.ninemo.clinical.model.entity;

import com.reejuven8.ninemo.clinical.model.enums.BagItemCategory;
import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import lombok.*;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "hospital_bag_items")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class HospitalBagItem {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "pregnancy_profile_id", nullable = false)
    private UUID pregnancyProfileId;

    @Column(name = "item_name", nullable = false, length = 200)
    private String itemName;

    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Enumerated(EnumType.STRING)
    @Column(name = "category", nullable = false, columnDefinition = "bag_item_category")
    private BagItemCategory category = BagItemCategory.OTHER;

    @Column(name = "is_packed", nullable = false)
    private boolean isPacked = false;

    @Column(name = "is_custom_item", nullable = false)
    private boolean isCustomItem = false;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder = 0;

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
