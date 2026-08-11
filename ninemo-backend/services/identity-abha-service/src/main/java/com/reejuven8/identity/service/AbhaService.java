package com.reejuven8.identity.service;

import com.reejuven8.identity.config.AbdmConfig;
import com.reejuven8.identity.model.dto.AbhaAddressRequest;
import com.reejuven8.identity.model.dto.AbhaEnrollmentResponse;
import com.reejuven8.identity.model.dto.AbhaOtpVerifyRequest;
import com.reejuven8.identity.model.entity.User;
import com.reejuven8.identity.repository.UserRepository;
import com.reejuven8.identity.security.RsaEncryptionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.time.Duration;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class AbhaService {

    private static final String TXN_PREFIX = "abdm:txn:";
    private static final Duration TXN_TTL = Duration.ofMinutes(5);

    private final RestClient abdmRestClient;
    private final AbdmConfig abdmConfig;
    private final RsaEncryptionService rsaEncryptionService;
    private final StringRedisTemplate redis;
    private final UserRepository userRepository;

    public AbhaEnrollmentResponse initiateEnrollment(String mobileNumber) {
        // Fetch ABDM public key then RSA-encrypt mobile number
        String publicKey = fetchAbdmPublicKey();
        String encryptedMobile = rsaEncryptionService.encrypt(mobileNumber, publicKey);

        Map<?, ?> response = abdmRestClient.post()
            .uri("/api/v3/enrollment/request/otp")
            .header("Authorization", "Bearer " + fetchAbdmAccessToken())
            .body(Map.of(
                "scope", "abha-enrol",
                "loginHint", "mobile",
                "loginId", encryptedMobile,
                "otpSystem", "abdm"
            ))
            .retrieve()
            .body(Map.class);

        String txnId = (String) response.get("txnId");
        redis.opsForValue().set(TXN_PREFIX + txnId, mobileNumber, TXN_TTL);
        return new AbhaEnrollmentResponse(txnId, "OTP sent to registered mobile");
    }

    public AbhaEnrollmentResponse verifyOtp(AbhaOtpVerifyRequest request) {
        Map<?, ?> response = abdmRestClient.post()
            .uri("/api/v3/enrollment/enrol/byAadhaar")
            .header("Authorization", "Bearer " + fetchAbdmAccessToken())
            .body(Map.of(
                "scope", "abha-enrol",
                "txnId", request.txnId(),
                "otp", request.encryptedOtp()
            ))
            .retrieve()
            .body(Map.class);

        String newTxnId = (String) response.get("txnId");
        return new AbhaEnrollmentResponse(newTxnId, "Enrollment OTP verified");
    }

    public void linkAbhaAddress(AbhaAddressRequest request, UUID userId) {
        Map<?, ?> response = abdmRestClient.post()
            .uri("/api/v3/enrollment/enrol/suggestion")
            .header("Authorization", "Bearer " + fetchAbdmAccessToken())
            .body(Map.of("txnId", request.txnId(), "abhaAddress", request.preferredAddress()))
            .retrieve()
            .body(Map.class);

        String abhaAddress = (String) response.get("abhaAddress");
        userRepository.findById(userId).ifPresent(user -> {
            user.setAbhaAddress(abhaAddress);
            userRepository.save(user);
        });
    }

    public void storeTxnId(String txnId, String metadata) {
        redis.opsForValue().set(TXN_PREFIX + txnId, metadata, TXN_TTL);
    }

    public String lookupTxnId(String txnId) {
        return redis.opsForValue().get(TXN_PREFIX + txnId);
    }

    private String fetchAbdmPublicKey() {
        Map<?, ?> response = abdmRestClient.get()
            .uri("/api/v3/profile/public/certificate")
            .retrieve()
            .body(Map.class);
        return (String) response.get("publicKey");
    }

    private String fetchAbdmAccessToken() {
        Map<?, ?> response = abdmRestClient.post()
            .uri("/api/v3/sessions")
            .body(Map.of(
                "clientId", abdmConfig.getClientId(),
                "clientSecret", abdmConfig.getClientSecret(),
                "grantType", "client_credentials"
            ))
            .retrieve()
            .body(Map.class);
        return (String) response.get("accessToken");
    }
}
