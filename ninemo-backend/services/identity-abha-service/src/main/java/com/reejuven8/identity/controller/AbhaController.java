package com.reejuven8.identity.controller;

import com.reejuven8.common.dto.ApiResponse;
import com.reejuven8.identity.model.dto.*;
import com.reejuven8.identity.service.AbhaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/identity/abha")
@RequiredArgsConstructor
@Tag(name = "ABHA")
public class AbhaController {

    private final AbhaService abhaService;

    @Operation(summary = "Initiate ABHA enrollment — sends OTP to mobile")
    @PostMapping("/enroll/otp/generate")
    public ResponseEntity<ApiResponse<AbhaEnrollmentResponse>> generateEnrollmentOtp(
        @Valid @RequestBody AbhaOtpRequest request
    ) {
        AbhaEnrollmentResponse response = abhaService.initiateEnrollment(request.mobileNumber());
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @Operation(summary = "Verify OTP and complete ABHA enrollment")
    @PostMapping("/enroll/otp/verify")
    public ResponseEntity<ApiResponse<AbhaEnrollmentResponse>> verifyEnrollmentOtp(
        @Valid @RequestBody AbhaOtpVerifyRequest request
    ) {
        AbhaEnrollmentResponse response = abhaService.verifyOtp(request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @Operation(summary = "Set ABHA address (PHR address) for a user")
    @PostMapping("/enroll/address")
    public ResponseEntity<ApiResponse<Void>> createAbhaAddress(
        @Valid @RequestBody AbhaAddressRequest request,
        @RequestHeader("X-User-Id") String userId
    ) {
        abhaService.linkAbhaAddress(request, UUID.fromString(userId));
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @Operation(summary = "Scan ABHA QR code to link existing health ID")
    @PostMapping("/scan")
    public ResponseEntity<ApiResponse<Void>> scan(@Valid @RequestBody AbhaScanRequest request) {
        throw new UnsupportedOperationException("ABHA QR scan — Phase 2");
    }
}
