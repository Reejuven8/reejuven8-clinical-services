package com.reejuven8.identity.repository;
import com.reejuven8.identity.model.entity.PatientProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.UUID;
public interface PatientProfileRepository extends JpaRepository<PatientProfile, UUID> {
    Optional<PatientProfile> findByUserId(UUID userId);
}
