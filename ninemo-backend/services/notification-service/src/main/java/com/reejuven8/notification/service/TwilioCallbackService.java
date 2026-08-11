package com.reejuven8.notification.service;

import com.reejuven8.notification.model.entity.NotificationStatus;
import com.reejuven8.notification.repository.NotificationLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
@RequiredArgsConstructor
@Slf4j
public class TwilioCallbackService {

    private final NotificationLogRepository logRepository;

    /**
     * Applies a Twilio status callback to the matching NotificationLog.
     * Idempotent and monotonic: statuses only advance (SENT → DELIVERED/FAILED);
     * a repeated or out-of-order callback never regresses a terminal state.
     */
    @Transactional
    public void applyStatus(String messageSid, String messageStatus, String errorCode) {
        logRepository.findByExternalMessageId(messageSid).ifPresentOrElse(entry -> {
            if (entry.getStatus() == NotificationStatus.DELIVERED) {
                return; // already terminal — idempotent no-op
            }
            switch (messageStatus) {
                case "delivered", "read" -> {
                    entry.setStatus(NotificationStatus.DELIVERED);
                    entry.setDeliveredAt(Instant.now());
                }
                case "failed", "undelivered" -> {
                    entry.setStatus(NotificationStatus.FAILED);
                    entry.setFailureReason("Twilio status=" + messageStatus
                        + (errorCode != null ? " errorCode=" + errorCode : ""));
                }
                default -> {
                    // queued/sending/sent — intermediate, nothing to record
                    return;
                }
            }
            logRepository.save(entry);
            log.info("Twilio callback applied sid={} status={}", messageSid, messageStatus);
        }, () -> log.warn("Twilio callback for unknown sid={}", messageSid));
    }
}
