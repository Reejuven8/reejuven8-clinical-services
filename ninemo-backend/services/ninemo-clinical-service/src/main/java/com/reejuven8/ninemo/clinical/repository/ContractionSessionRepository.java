package com.reejuven8.ninemo.clinical.repository;

import com.reejuven8.ninemo.clinical.model.document.ContractionSession;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ContractionSessionRepository extends MongoRepository<ContractionSession, String> {
    List<ContractionSession> findByPatientIdOrderBySessionStartDesc(String patientId);
}
