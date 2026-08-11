package com.reejuven8.ninemo.clinical.controller;

import com.reejuven8.common.dto.ApiResponse;
import com.reejuven8.ninemo.clinical.model.document.SymptomLog;
import com.reejuven8.ninemo.clinical.model.dto.SymptomLogRequest;
import com.reejuven8.ninemo.clinical.model.dto.SymptomLogResponse;
import com.reejuven8.ninemo.clinical.service.SymptomService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/ninemo/symptoms")
@RequiredArgsConstructor
@Tag(name = "Symptoms")
public class SymptomController {

    private final SymptomService symptomService;

    @Operation(summary = "Log a symptom entry and run triage evaluation")
    @PostMapping
    public ResponseEntity<ApiResponse<SymptomLogResponse>> logSymptoms(
        @RequestHeader("X-User-Id") UUID userId,
        @Valid @RequestBody SymptomLogRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.success(symptomService.logSymptoms(userId, request)));
    }

    @Operation(summary = "Get symptom history for the authenticated patient")
    @GetMapping
    public ResponseEntity<ApiResponse<List<SymptomLog>>> getSymptomHistory(
        @RequestHeader("X-User-Id") UUID userId
    ) {
        return ResponseEntity.ok(ApiResponse.success(symptomService.getHistory(userId)));
    }
}
