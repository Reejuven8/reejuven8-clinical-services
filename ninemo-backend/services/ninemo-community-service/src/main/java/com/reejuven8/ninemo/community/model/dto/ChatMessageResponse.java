package com.reejuven8.ninemo.community.model.dto;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;

@Data @Builder
public class ChatMessageResponse {
    private String id;
    private String clubId;
    private String channelId;
    private String senderId;
    private String senderAlias;
    private String messageType;
    private String messageBody;
    private String replyToMessageId;
    private String imageUrl;
    private Instant sentAt;
}
