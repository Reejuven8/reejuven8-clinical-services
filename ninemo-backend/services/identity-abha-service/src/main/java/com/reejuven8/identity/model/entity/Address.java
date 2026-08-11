package com.reejuven8.identity.model.entity;

import com.reejuven8.identity.model.enums.AddressType;
import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import lombok.*;
import org.hibernate.annotations.UuidGenerator;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "addresses")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Address {
    @Id @UuidGenerator @Column(updatable = false, nullable = false) private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Enumerated(EnumType.STRING)
    @Column(name = "address_type", nullable = false)
    private AddressType addressType;

    @Column(name = "address_line_1", nullable = false, length = 255) private String addressLine1;
    @Column(name = "address_line_2", length = 255) private String addressLine2;
    @Column(name = "city", nullable = false, length = 100) private String city;
    @Column(name = "state", nullable = false, length = 100) private String state;
    @Column(name = "pincode", nullable = false, length = 10) private String pincode;
    @Column(name = "country", nullable = false, length = 100) @Builder.Default private String country = "India";

    @Column(name = "created_at", nullable = false, updatable = false) @Builder.Default private Instant createdAt = Instant.now();
    @Column(name = "updated_at", nullable = false) @Builder.Default private Instant updatedAt = Instant.now();
    @PreUpdate void onUpdate() { this.updatedAt = Instant.now(); }
}
