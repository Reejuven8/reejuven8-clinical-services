package com.reejuven8.identity.controller;

import com.reejuven8.common.dto.ApiResponse;
import com.reejuven8.identity.model.dto.*;
import com.reejuven8.identity.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/identity/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication")
public class AuthController {

    private final AuthService authService;

    @Operation(summary = "Send OTP to mobile number")
    @PostMapping("/otp/send")
    public ResponseEntity<ApiResponse<Void>> sendOtp(@Valid @RequestBody OtpRequest request) {
        authService.sendOtp(request);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @Operation(summary = "Login with phone number and OTP")
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<TokenResponse>> login(@Valid @RequestBody LoginRequest request) {
        TokenResponse tokens = authService.loginWithOtp(request.phoneNumber(), request.otp());
        return ResponseEntity.ok(ApiResponse.success(tokens));
    }

    @Operation(summary = "Register a new patient account")
    @PostMapping("/register")
    public ResponseEntity<ApiResponse<TokenResponse>> register(@Valid @RequestBody RegisterRequest request) {
        TokenResponse tokens = authService.register(request);
        return ResponseEntity.ok(ApiResponse.success(tokens));
    }

    @Operation(summary = "Refresh access token using refresh token")
    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<TokenResponse>> refresh(@RequestHeader("Authorization") String bearerToken) {
        TokenResponse tokens = authService.refresh(bearerToken);
        return ResponseEntity.ok(ApiResponse.success(tokens));
    }

    @Operation(summary = "Invalidate refresh token and log out")
    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(@RequestHeader("Authorization") String bearerToken) {
        authService.logout(bearerToken);
        return ResponseEntity.ok(ApiResponse.success(null));
    }
}
