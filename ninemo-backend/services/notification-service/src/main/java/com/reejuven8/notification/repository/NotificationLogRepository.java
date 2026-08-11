package com.reejuven8.notification.repository;
import com.reejuven8.notification.model.entity.NotificationLog;
import com.reejuven8.notification.model.entity.NotificationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;
public interface NotificationLogRepository extends JpaRepository<NotificationLog, UUID> {
    List<NotificationLog> findByUserIdOrderByCreatedAtDesc(UUID userId);
    List<NotificationLog> findByStatusIn(List<NotificationStatus> statuses);
    java.util.Optional<NotificationLog> findByExternalMessageId(String externalMessageId);
}
