package com.reejuven8.notification.service;

import com.reejuven8.notification.model.dto.DeviceRegistrationRequest;
import com.reejuven8.notification.model.entity.DeviceToken;
import com.reejuven8.notification.repository.DeviceTokenRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class DeviceTokenService {

    private final DeviceTokenRepository repository;

    /** Upsert by token — a device that changes owner (re-login) re-binds to the new user. */
    @Transactional
    public DeviceToken register(UUID userId, DeviceRegistrationRequest request) {
        DeviceToken token = repository.findByFcmToken(request.getFcmToken())
            .map(existing -> {
                existing.setUserId(userId);
                existing.setPlatform(request.getPlatform());
                return existing;
            })
            .orElseGet(() -> DeviceToken.builder()
                .userId(userId)
                .fcmToken(request.getFcmToken())
                .platform(request.getPlatform())
                .build());
        DeviceToken saved = repository.save(token);
        log.info("Device registered userId={} platform={}", userId, request.getPlatform());
        return saved;
    }

    @Transactional
    public void unregister(String fcmToken) {
        repository.deleteByFcmToken(fcmToken);
    }

    public List<String> tokensForUser(UUID userId) {
        return repository.findByUserId(userId).stream()
            .map(DeviceToken::getFcmToken)
            .toList();
    }
}
