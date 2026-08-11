package com.reejuven8.notification.model.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import lombok.*;
import org.hibernate.annotations.UuidGenerator;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "notification_logs")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class NotificationLog {
    @Id @UuidGenerator @Column(updatable = false, nullable = false) private UUID id;

    @Column(name = "user_id", nullable = false) private UUID userId;

    @Column(name = "channel", nullable = false)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Enumerated(EnumType.STRING)
    private NotificationChannel channel;

    @Column(name = "event_type", nullable = false, length = 100) private String eventType;

    @Column(name = "status", nullable = false)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Enumerated(EnumType.STRING)
    @Builder.Default private NotificationStatus status = NotificationStatus.PENDING;

    @Column(name = "title", length = 500) private String title;
    @Column(name = "message_body", nullable = false, columnDefinition = "TEXT") private String messageBody;
    @Column(name = "retry_count", nullable = false) @Builder.Default private Integer retryCount = 0;
    @Column(name = "max_retries", nullable = false) @Builder.Default private Integer maxRetries = 4;
    @Column(name = "external_message_id", length = 255) private String externalMessageId;
    @Column(name = "failure_reason", columnDefinition = "TEXT") private String failureReason;
    @Column(name = "sent_at") private Instant sentAt;
    @Column(name = "delivered_at") private Instant deliveredAt;
    @Column(name = "created_at", nullable = false, updatable = false) @Builder.Default private Instant createdAt = Instant.now();
}
