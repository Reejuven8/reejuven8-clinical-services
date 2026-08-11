package com.reejuven8.notification.controller;

import com.reejuven8.common.dto.ApiResponse;
import com.reejuven8.notification.model.dto.DeviceRegistrationRequest;
import com.reejuven8.notification.service.DeviceTokenService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/notifications/devices")
@RequiredArgsConstructor
@Tag(name = "Device Registration")
public class DeviceController {

    private final DeviceTokenService deviceTokenService;

    @Operation(summary = "Register (or re-bind) an FCM device token for push delivery")
    @PostMapping
    public ResponseEntity<ApiResponse<Void>> register(
        @RequestHeader("X-User-Id") UUID userId,
        @Valid @RequestBody DeviceRegistrationRequest request
    ) {
        deviceTokenService.register(userId, request);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @Operation(summary = "Unregister a device token (logout / token rotation)")
    @DeleteMapping
    public ResponseEntity<ApiResponse<Void>> unregister(@RequestParam String fcmToken) {
        deviceTokenService.unregister(fcmToken);
        return ResponseEntity.ok(ApiResponse.success(null));
    }
}
