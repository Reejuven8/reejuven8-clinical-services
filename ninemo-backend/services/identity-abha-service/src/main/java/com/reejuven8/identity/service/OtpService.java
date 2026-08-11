package com.reejuven8.identity.service;

import com.reejuven8.common.exception.UnauthorizedException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Duration;

@Service
@RequiredArgsConstructor
@Slf4j
public class OtpService {

    private static final String KEY_PREFIX = "otp:";
    private static final Duration OTP_TTL = Duration.ofMinutes(5);
    private static final int OTP_LENGTH = 6;

    private final StringRedisTemplate redis;
    private final SecureRandom secureRandom = new SecureRandom();

    public String generateAndStore(String phoneNumber) {
        String otp = generateOtp();
        redis.opsForValue().set(KEY_PREFIX + phoneNumber, otp, OTP_TTL);
        // In production, dispatch via SMS/WhatsApp; log at DEBUG only — never at INFO
        log.debug("OTP generated for {}", phoneNumber);
        return otp;
    }

    public void verifyAndConsume(String phoneNumber, String otp) {
        String stored = redis.opsForValue().get(KEY_PREFIX + phoneNumber);
        if (stored == null) {
            throw new UnauthorizedException("OTP expired or not sent");
        }
        if (!stored.equals(otp)) {
            throw new UnauthorizedException("Invalid OTP");
        }
        redis.delete(KEY_PREFIX + phoneNumber);
    }

    private String generateOtp() {
        int n = secureRandom.nextInt(900000) + 100000;
        return String.valueOf(n);
    }
}
