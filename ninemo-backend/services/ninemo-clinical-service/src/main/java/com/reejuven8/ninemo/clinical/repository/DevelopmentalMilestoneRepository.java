package com.reejuven8.ninemo.clinical.repository;

import com.reejuven8.ninemo.clinical.model.document.DevelopmentalMilestone;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DevelopmentalMilestoneRepository extends MongoRepository<DevelopmentalMilestone, String> {
    Optional<DevelopmentalMilestone> findByChildIdAndMonth(String childId, int month);
    List<DevelopmentalMilestone> findByChildIdOrderByMonthAsc(String childId);
}
