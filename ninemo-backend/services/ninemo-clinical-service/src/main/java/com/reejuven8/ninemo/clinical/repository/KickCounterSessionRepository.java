package com.reejuven8.ninemo.clinical.repository;

import com.reejuven8.ninemo.clinical.model.document.KickCounterSession;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface KickCounterSessionRepository extends MongoRepository<KickCounterSession, String> {
    List<KickCounterSession> findByPatientIdOrderBySessionStartDesc(String patientId);
}
