package com.reejuven8.ninemo.community.controller;

import com.reejuven8.common.dto.ApiResponse;
import com.reejuven8.ninemo.community.service.ChatMessageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/ninemo/community/clubs/{clubId}/channels/{channelId}/messages")
@RequiredArgsConstructor
@Tag(name = "Chat History")
public class ChatRestController {

    private final ChatMessageService chatMessageService;

    @Operation(summary = "Get paginated chat message history for a channel")
    @GetMapping
    public ResponseEntity<ApiResponse<Object>> getHistory(
            @PathVariable String clubId,
            @PathVariable String channelId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        return ResponseEntity.ok(ApiResponse.success(
            chatMessageService.getHistory(clubId, channelId, page, size)));
    }

    @Operation(summary = "Soft-delete a chat message (author only)")
    @DeleteMapping("/{messageId}")
    public ResponseEntity<ApiResponse<Object>> deleteMessage(
            @PathVariable String clubId,
            @PathVariable String channelId,
            @PathVariable String messageId,
            @RequestHeader("X-User-Id") String userId) {
        chatMessageService.softDelete(messageId, userId);
        return ResponseEntity.ok(ApiResponse.success(null));
    }
}
