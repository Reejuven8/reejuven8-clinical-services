package com.reejuven8.identity.model.entity;

import com.reejuven8.identity.model.enums.ConsentStatus;
import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import lombok.*;
import org.hibernate.annotations.UuidGenerator;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "user_consents")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class UserConsent {
    @Id @UuidGenerator @Column(updatable = false, nullable = false) private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "patient_id", nullable = false)
    private User patient;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "doctor_id", nullable = false)
    private User doctor;

    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Enumerated(EnumType.STRING)
    @Column(name = "consent_status", nullable = false)
    private ConsentStatus consentStatus;

    @Column(name = "abdm_consent_id", length = 255) private String abdmConsentId;
    @Column(name = "granted_at", nullable = false) private Instant grantedAt;
    @Column(name = "expires_at", nullable = false) private Instant expiresAt;
    @Column(name = "revoked_at") private Instant revokedAt;

    @Column(name = "created_at", nullable = false, updatable = false) @Builder.Default private Instant createdAt = Instant.now();
    @Column(name = "updated_at", nullable = false) @Builder.Default private Instant updatedAt = Instant.now();
    @PreUpdate void onUpdate() { this.updatedAt = Instant.now(); }
}
