package com.reejuven8.ninemo.clinical.repository;

import com.reejuven8.ninemo.clinical.model.document.GrowthMeasurement;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface GrowthMeasurementRepository extends MongoRepository<GrowthMeasurement, String> {
    List<GrowthMeasurement> findByChildIdOrderByMeasurementDateDesc(String childId);
    Optional<GrowthMeasurement> findTop1ByChildIdOrderByMeasurementDateDesc(String childId);
}
