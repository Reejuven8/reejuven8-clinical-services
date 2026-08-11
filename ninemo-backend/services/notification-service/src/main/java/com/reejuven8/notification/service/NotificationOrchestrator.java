package com.reejuven8.notification.service;

import com.reejuven8.notification.model.dto.NotificationRequest;
import com.reejuven8.notification.model.entity.NotificationChannel;
import com.reejuven8.notification.model.entity.NotificationLog;
import com.reejuven8.notification.model.entity.NotificationStatus;
import com.reejuven8.notification.repository.NotificationLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationOrchestrator {

    private final WhatsAppService whatsAppService;
    private final SmsService smsService;
    private final PushNotificationService pushService;
    private final DeviceTokenService deviceTokenService;
    private final NotificationLogRepository logRepository;

    public void dispatch(NotificationRequest request) {
        List<NotificationChannel> channels = request.getChannels() != null
            ? request.getChannels()
            : List.of(NotificationChannel.PUSH, NotificationChannel.WHATSAPP);

        for (NotificationChannel channel : channels) {
            NotificationLog entry = NotificationLog.builder()
                .userId(request.getUserId())
                .channel(channel)
                .eventType(request.getEventType())
                .title(request.getTitle())
                .messageBody(request.getBody())
                .build();

            try {
                String externalId = switch (channel) {
                    case WHATSAPP -> whatsAppService.send(request.getPhoneNumber(), request.getBody());
                    case SMS      -> smsService.send(request.getPhoneNumber(), request.getBody());
                    case PUSH     -> sendPush(request);
                    case EMAIL    -> { log.warn("Email not yet implemented"); yield "SKIPPED"; }
                };
                entry.setExternalMessageId(externalId);
                entry.setStatus("SKIPPED".equals(externalId) ? NotificationStatus.SKIPPED : NotificationStatus.SENT);
                entry.setSentAt(Instant.now());
            } catch (Exception e) {
                entry.setStatus(NotificationStatus.FAILED);
                entry.setFailureReason(e.getMessage());
            }

            logRepository.save(entry);
        }
    }

    /**
     * Sends to every registered device for the user; falls back to the request's
     * explicit fcmToken (legacy path) when no device is registered.
     */
    private String sendPush(NotificationRequest request) {
        List<String> tokens = request.getUserId() != null
            ? deviceTokenService.tokensForUser(request.getUserId())
            : List.of();
        if (tokens.isEmpty()) {
            return pushService.send(request.getFcmToken(), request.getTitle(), request.getBody());
        }
        List<String> messageIds = tokens.stream()
            .map(token -> pushService.send(token, request.getTitle(), request.getBody()))
            .toList();
        return String.join(",", messageIds);
    }
}
