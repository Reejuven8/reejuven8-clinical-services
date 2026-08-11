package com.reejuven8.identity.model.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UuidGenerator;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "doctor_profiles")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class DoctorProfile {
    @Id @UuidGenerator @Column(updatable = false, nullable = false) private UUID id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", unique = true, nullable = false)
    private User user;

    @Column(name = "medical_license_number", unique = true, nullable = false, length = 50)
    private String medicalLicenseNumber;

    @Column(name = "specialization", nullable = false, length = 100)
    private String specialization;

    @Column(name = "qualifications", nullable = false, length = 200)
    private String qualifications;

    @Column(name = "years_of_experience")
    private Integer yearsOfExperience;

    @Column(name = "consultation_fee", nullable = false, precision = 10, scale = 2)
    @Builder.Default private BigDecimal consultationFee = BigDecimal.ZERO;

    @Column(name = "bio", columnDefinition = "TEXT")
    private String bio;

    @Column(name = "digital_signature_url", length = 500)
    private String digitalSignatureUrl;

    @Column(name = "is_accepting_patients", nullable = false)
    @Builder.Default private Boolean isAcceptingPatients = true;

    @Column(name = "created_at", nullable = false, updatable = false) @Builder.Default private Instant createdAt = Instant.now();
    @Column(name = "updated_at", nullable = false) @Builder.Default private Instant updatedAt = Instant.now();
    @PreUpdate void onUpdate() { this.updatedAt = Instant.now(); }
}
