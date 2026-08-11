package com.reejuven8.ninemo.clinical.repository;

import com.reejuven8.ninemo.clinical.model.entity.PregnancyProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface PregnancyProfileRepository extends JpaRepository<PregnancyProfile, UUID> {
    Optional<PregnancyProfile> findByUserIdAndIsActiveTrue(UUID userId);
}
