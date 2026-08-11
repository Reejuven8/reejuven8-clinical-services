package com.reejuven8.ninemo.clinical.controller;

import com.reejuven8.common.dto.ApiResponse;
import com.reejuven8.ninemo.clinical.model.document.VitalsLog;
import com.reejuven8.ninemo.clinical.model.dto.VitalsLogRequest;
import com.reejuven8.ninemo.clinical.service.VitalsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/ninemo/vitals")
@RequiredArgsConstructor
@Tag(name = "Vitals")
public class VitalsController {

    private final VitalsService vitalsService;

    @Operation(summary = "Record a vitals measurement (weight, blood pressure, etc.)")
    @PostMapping
    public ResponseEntity<ApiResponse<VitalsLog>> logVital(@RequestHeader("X-User-Id") UUID userId,
                                                           @Valid @RequestBody VitalsLogRequest request) {
        return ResponseEntity.ok(ApiResponse.success(vitalsService.logVital(userId, request)));
    }

    @Operation(summary = "Get vitals history for a specific type (WEIGHT, BLOOD_PRESSURE)")
    @GetMapping("/{vitalType}")
    public ResponseEntity<ApiResponse<List<VitalsLog>>> getHistory(@RequestHeader("X-User-Id") UUID userId,
                                                                   @PathVariable String vitalType) {
        return ResponseEntity.ok(ApiResponse.success(vitalsService.getVitalsHistory(userId, vitalType)));
    }
}
