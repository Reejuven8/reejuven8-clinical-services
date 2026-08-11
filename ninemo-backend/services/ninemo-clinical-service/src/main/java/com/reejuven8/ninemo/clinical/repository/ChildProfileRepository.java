package com.reejuven8.ninemo.clinical.repository;

import com.reejuven8.ninemo.clinical.model.entity.ChildProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ChildProfileRepository extends JpaRepository<ChildProfile, UUID> {
    Optional<ChildProfile> findByParentUserIdAndIsActiveTrue(UUID parentUserId);
    Optional<ChildProfile> findByPregnancyProfileId(UUID pregnancyProfileId);

    /** NM-B-169: a parent can have several children — the Optional variant above breaks then. */
    List<ChildProfile> findByParentUserIdAndIsActiveTrueOrderByDateOfBirthDesc(UUID parentUserId);
}
