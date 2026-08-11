package com.reejuven8.ninemo.clinical.repository;

import com.reejuven8.ninemo.clinical.model.document.SymptomLog;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SymptomLogRepository extends MongoRepository<SymptomLog, String> {
    List<SymptomLog> findByPatientIdOrderByLoggedAtDesc(String patientId);
    List<SymptomLog> findTop5ByPatientIdOrderByLoggedAtDesc(String patientId);
}
