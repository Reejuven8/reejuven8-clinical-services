package com.reejuven8.identity.repository;
import com.reejuven8.identity.model.entity.UserConsent;
import com.reejuven8.identity.model.enums.ConsentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
public interface UserConsentRepository extends JpaRepository<UserConsent, UUID> {
    List<UserConsent> findByPatientId(UUID patientId);
    Optional<UserConsent> findByPatientIdAndDoctorIdAndConsentStatusAndExpiresAtAfter(
        UUID patientId, UUID doctorId, ConsentStatus status, Instant now);
}
