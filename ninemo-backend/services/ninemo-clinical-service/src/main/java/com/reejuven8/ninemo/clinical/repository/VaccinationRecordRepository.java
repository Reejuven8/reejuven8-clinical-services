package com.reejuven8.ninemo.clinical.repository;

import com.reejuven8.ninemo.clinical.model.entity.VaccinationRecord;
import com.reejuven8.ninemo.clinical.model.enums.VaccinationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface VaccinationRecordRepository extends JpaRepository<VaccinationRecord, UUID> {
    List<VaccinationRecord> findByChildIdOrderByScheduledDateAsc(UUID childId);
    List<VaccinationRecord> findByChildIdAndStatus(UUID childId, VaccinationStatus status);
}
