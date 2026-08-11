package com.reejuven8.ninemo.clinical.repository;

import com.reejuven8.ninemo.clinical.model.document.VitalsLog;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface VitalsLogRepository extends MongoRepository<VitalsLog, String> {
    List<VitalsLog> findByPatientIdAndVitalTypeOrderByLoggedAtDesc(String patientId, String vitalType);
    List<VitalsLog> findTop1ByPatientIdAndVitalTypeOrderByLoggedAtDesc(String patientId, String vitalType);
}
