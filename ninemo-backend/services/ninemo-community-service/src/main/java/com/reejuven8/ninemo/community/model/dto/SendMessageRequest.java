package com.reejuven8.ninemo.community.model.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class SendMessageRequest {
    // senderId intentionally absent — sender identity comes from the authenticated
    // STOMP principal (JWT), never from the client payload.
    @NotBlank
    private String senderAlias;

    @NotBlank
    private String messageBody;

    private String messageType = "TEXT";
    private String replyToMessageId;
    private String imageUrl;
}
