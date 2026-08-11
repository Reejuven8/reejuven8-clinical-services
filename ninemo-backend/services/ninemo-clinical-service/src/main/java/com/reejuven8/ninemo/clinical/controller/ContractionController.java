package com.reejuven8.ninemo.clinical.controller;

import com.reejuven8.common.dto.ApiResponse;
import com.reejuven8.ninemo.clinical.model.dto.ContractionRequest;
import com.reejuven8.ninemo.clinical.model.dto.ContractionSessionResponse;
import com.reejuven8.ninemo.clinical.service.ContractionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/ninemo/contractions")
@RequiredArgsConstructor
@Tag(name = "Contraction Timer")
public class ContractionController {

    private final ContractionService contractionService;

    @Operation(summary = "Start a new contraction timing session")
    @PostMapping("/sessions")
    public ResponseEntity<ApiResponse<ContractionSessionResponse>> startSession(
        @RequestHeader("X-User-Id") UUID userId
    ) {
        return ResponseEntity.ok(ApiResponse.success(contractionService.startSession(userId)));
    }

    @Operation(summary = "Record a contraction event with duration")
    @PutMapping("/sessions/{sessionId}/contraction")
    public ResponseEntity<ApiResponse<ContractionSessionResponse>> recordContraction(
        @RequestHeader("X-User-Id") UUID userId,
        @PathVariable String sessionId,
        @Valid @RequestBody ContractionRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.success(
            contractionService.recordContraction(userId, sessionId, request)
        ));
    }

    @Operation(summary = "End a contraction timing session")
    @PutMapping("/sessions/{sessionId}/end")
    public ResponseEntity<ApiResponse<ContractionSessionResponse>> endSession(
        @PathVariable String sessionId
    ) {
        return ResponseEntity.ok(ApiResponse.success(contractionService.endSession(sessionId)));
    }
}
