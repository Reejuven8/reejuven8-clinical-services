package com.reejuven8.notification.model.dto;

import com.reejuven8.notification.model.entity.DevicePlatform;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class DeviceRegistrationRequest {
    @NotBlank
    private String fcmToken;

    @NotNull
    private DevicePlatform platform;
}
