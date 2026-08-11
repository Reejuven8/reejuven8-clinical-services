package com.reejuven8.notification.service;

import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.Notification;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class PushNotificationService {

    public String send(String fcmToken, String title, String body) {
        if (fcmToken == null || fcmToken.isBlank()) {
            log.warn("No FCM token provided — skipping push");
            return "SKIPPED";
        }
        try {
            Message message = Message.builder()
                .setToken(fcmToken)
                .setNotification(Notification.builder()
                    .setTitle(title)
                    .setBody(body)
                    .build())
                .build();
            String messageId = FirebaseMessaging.getInstance().send(message);
            log.info("FCM sent messageId={}", messageId);
            return messageId;
        } catch (Exception e) {
            log.error("FCM send failed error={}", e.getMessage());
            throw new RuntimeException("Push notification failed", e);
        }
    }
}
