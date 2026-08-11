package com.reejuven8.identity.repository;
import com.reejuven8.identity.model.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {
    Optional<User> findByPhoneNumber(String phoneNumber);
    Optional<User> findByAbhaAddress(String abhaAddress);
    boolean existsByPhoneNumber(String phoneNumber);
    boolean existsByAbhaAddress(String abhaAddress);
}
