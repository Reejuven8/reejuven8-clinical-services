package com.reejuven8.notification.model.dto;

import com.reejuven8.notification.model.entity.NotificationChannel;
import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.UUID;

@Data @Builder
public class NotificationRequest {
    private UUID userId;
    private String title;
    private String body;
    private String eventType;
    private List<NotificationChannel> channels;
    private String phoneNumber;
    private String fcmToken;
}
