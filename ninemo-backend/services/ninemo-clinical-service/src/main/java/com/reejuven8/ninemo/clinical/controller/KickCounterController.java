package com.reejuven8.ninemo.clinical.controller;

import com.reejuven8.common.dto.ApiResponse;
import com.reejuven8.ninemo.clinical.model.dto.KickSessionResponse;
import com.reejuven8.ninemo.clinical.service.KickCounterService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/ninemo/kick-counter")
@RequiredArgsConstructor
@Tag(name = "Kick Counter")
public class KickCounterController {

    private final KickCounterService kickCounterService;

    @Operation(summary = "Start a new kick counting session")
    @PostMapping("/sessions")
    public ResponseEntity<ApiResponse<KickSessionResponse>> startSession(
        @RequestHeader("X-User-Id") UUID userId
    ) {
        return ResponseEntity.ok(ApiResponse.success(kickCounterService.startSession(userId)));
    }

    @Operation(summary = "Record a kick event in the active session")
    @PutMapping("/sessions/{sessionId}/kick")
    public ResponseEntity<ApiResponse<KickSessionResponse>> recordKick(
        @RequestHeader("X-User-Id") UUID userId,
        @PathVariable String sessionId
    ) {
        return ResponseEntity.ok(ApiResponse.success(kickCounterService.recordKick(userId, sessionId)));
    }

    @Operation(summary = "End a kick counting session")
    @PutMapping("/sessions/{sessionId}/end")
    public ResponseEntity<ApiResponse<KickSessionResponse>> endSession(
        @RequestHeader("X-User-Id") UUID userId,
        @PathVariable String sessionId
    ) {
        return ResponseEntity.ok(ApiResponse.success(kickCounterService.endSession(userId, sessionId)));
    }
}
