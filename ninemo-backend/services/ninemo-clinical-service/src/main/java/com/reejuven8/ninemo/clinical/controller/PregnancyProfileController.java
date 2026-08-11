package com.reejuven8.ninemo.clinical.controller;

import com.reejuven8.common.dto.ApiResponse;
import com.reejuven8.ninemo.clinical.model.dto.PregnancyProfileRequest;
import com.reejuven8.ninemo.clinical.model.dto.PregnancyProfileResponse;
import com.reejuven8.ninemo.clinical.service.PregnancyProfileService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * NM-B-167 / IS-018. X-User-Id is injected by the api-gateway after JWT validation —
 * the profile is always scoped to the caller, never to a client-supplied id.
 */
@RestController
@RequestMapping("/api/v1/ninemo/profiles/pregnancy")
@RequiredArgsConstructor
@Tag(name = "Pregnancy Profile")
public class PregnancyProfileController {

    private final PregnancyProfileService pregnancyProfileService;

    @Operation(summary = "Create the caller's pregnancy profile (onboarding); computes EDD and baseline BMI")
    @PostMapping
    public ResponseEntity<ApiResponse<PregnancyProfileResponse>> create(
            @RequestHeader("X-User-Id") UUID userId,
            @Valid @RequestBody PregnancyProfileRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.success(pregnancyProfileService.create(userId, request)));
    }

    @Operation(summary = "Get the caller's active pregnancy profile (404 when onboarding is not done)")
    @GetMapping
    public ResponseEntity<ApiResponse<PregnancyProfileResponse>> getActive(
            @RequestHeader("X-User-Id") UUID userId) {
        return ResponseEntity.ok(ApiResponse.success(pregnancyProfileService.getActive(userId)));
    }
}
