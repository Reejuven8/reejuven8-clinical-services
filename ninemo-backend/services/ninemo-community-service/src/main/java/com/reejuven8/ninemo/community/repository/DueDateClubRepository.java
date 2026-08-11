package com.reejuven8.ninemo.community.repository;
import com.reejuven8.ninemo.community.model.DueDateClub;
import org.springframework.data.mongodb.repository.MongoRepository;
import java.util.Optional;
public interface DueDateClubRepository extends MongoRepository<DueDateClub, String> {
    Optional<DueDateClub> findByDueDateMonth(String dueDateMonth);
}
