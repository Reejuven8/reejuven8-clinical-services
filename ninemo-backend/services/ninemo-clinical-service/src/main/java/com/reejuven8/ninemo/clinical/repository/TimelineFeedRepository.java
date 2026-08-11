package com.reejuven8.ninemo.clinical.repository;

import com.reejuven8.ninemo.clinical.model.document.TimelineFeed;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TimelineFeedRepository extends MongoRepository<TimelineFeed, String> {
    Optional<TimelineFeed> findByPregnancyProfileIdAndGestationalWeek(String pregnancyProfileId, int gestationalWeek);
}
