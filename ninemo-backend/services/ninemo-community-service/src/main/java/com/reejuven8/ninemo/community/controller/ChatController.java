package com.reejuven8.ninemo.community.controller;

import com.reejuven8.ninemo.community.model.ChatMessage;
import com.reejuven8.ninemo.community.model.dto.SendMessageRequest;
import com.reejuven8.ninemo.community.service.ChatMessageService;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import java.security.Principal;

@Controller
@RequiredArgsConstructor
public class ChatController {

    private final SimpMessagingTemplate messagingTemplate;
    private final ChatMessageService chatMessageService;

    // STOMP: client sends to /app/chat.send/{clubId}/{channelId}
    // Server broadcasts to /topic/club.{clubId}.{channelId}
    // Sender identity = authenticated principal (JWT on CONNECT), never the payload
    @MessageMapping("/chat.send/{clubId}/{channelId}")
    public void sendMessage(@DestinationVariable String clubId,
                            @DestinationVariable String channelId,
                            @Payload SendMessageRequest request,
                            Principal principal) {
        ChatMessage saved = chatMessageService.send(clubId, channelId, principal.getName(), request);
        messagingTemplate.convertAndSend(
            "/topic/club." + clubId + "." + channelId,
            chatMessageService.toResponse(saved)
        );
    }
}
