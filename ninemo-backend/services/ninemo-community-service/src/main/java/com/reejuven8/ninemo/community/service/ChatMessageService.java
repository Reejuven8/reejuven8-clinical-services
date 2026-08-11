package com.reejuven8.ninemo.community.service;

import com.reejuven8.ninemo.community.model.ChatMessage;
import com.reejuven8.ninemo.community.model.dto.ChatMessageResponse;
import com.reejuven8.ninemo.community.model.dto.SendMessageRequest;
import com.reejuven8.ninemo.community.repository.ChatMessageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ChatMessageService {

    private final ChatMessageRepository chatMessageRepository;

    public ChatMessage send(String clubId, String channelId, String senderId, SendMessageRequest request) {
        ChatMessage msg = ChatMessage.builder()
            .clubId(clubId)
            .channelId(channelId)
            .senderId(senderId)
            .senderAlias(request.getSenderAlias())
            .messageType(request.getMessageType() != null ? request.getMessageType() : "TEXT")
            .messageBody(request.getMessageBody())
            .replyToMessageId(request.getReplyToMessageId())
            .imageUrl(request.getImageUrl())
            .build();
        return chatMessageRepository.save(msg);
    }

    public Page<ChatMessageResponse> getHistory(String clubId, String channelId, int page, int size) {
        return chatMessageRepository
            .findByClubIdAndChannelIdAndIsDeletedFalseOrderBySentAtDesc(
                clubId, channelId, PageRequest.of(page, size))
            .map(this::toResponse);
    }

    public void softDelete(String messageId, String requesterId) {
        chatMessageRepository.findById(messageId).ifPresent(msg -> {
            if (!msg.getSenderId().equals(requesterId)) {
                throw new IllegalStateException("Not authorized to delete message " + messageId);
            }
            msg.setIsDeleted(true);
            chatMessageRepository.save(msg);
        });
    }

    public ChatMessageResponse toResponse(ChatMessage msg) {
        return ChatMessageResponse.builder()
            .id(msg.getId())
            .clubId(msg.getClubId())
            .channelId(msg.getChannelId())
            .senderId(msg.getSenderId())
            .senderAlias(msg.getSenderAlias())
            .messageType(msg.getMessageType())
            .messageBody(msg.getMessageBody())
            .replyToMessageId(msg.getReplyToMessageId())
            .imageUrl(msg.getImageUrl())
            .sentAt(msg.getSentAt())
            .build();
    }
}
