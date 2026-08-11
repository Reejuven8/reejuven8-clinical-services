package com.reejuven8.identity.service;

import com.reejuven8.common.event.ConsentGrantedEvent;
import com.reejuven8.common.exception.ResourceNotFoundException;
import com.reejuven8.common.exception.UnauthorizedException;
import com.reejuven8.identity.model.dto.ConsentRequest;
import com.reejuven8.identity.model.dto.ConsentResponse;
import com.reejuven8.identity.model.entity.User;
import com.reejuven8.identity.model.entity.UserConsent;
import com.reejuven8.identity.model.enums.ConsentStatus;
import com.reejuven8.identity.repository.UserConsentRepository;
import com.reejuven8.identity.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.slf4j.MDC;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ConsentService {

    private static final String CONSENT_TOPIC = "abdm.consent.granted";

    private final UserConsentRepository consentRepository;
    private final UserRepository userRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Transactional
    public ConsentResponse grantConsent(UUID patientId, ConsentRequest request) {
        User patient = userRepository.findById(patientId)
            .orElseThrow(() -> new ResourceNotFoundException("User", patientId.toString()));
        User doctor = userRepository.findById(UUID.fromString(request.doctorId()))
            .orElseThrow(() -> new ResourceNotFoundException("User", request.doctorId()));

        UserConsent consent = UserConsent.builder()
            .patient(patient)
            .doctor(doctor)
            .consentStatus(ConsentStatus.GRANTED)
            .grantedAt(Instant.now())
            .expiresAt(request.expiresAt())
            .build();
        consent = consentRepository.save(consent);

        ConsentGrantedEvent event = ConsentGrantedEvent.builder()
            .source("identity-abha-service")
            .correlationId(MDC.get("correlationId"))
            .consentId(consent.getId().toString())
            .patientId(patientId.toString())
            .doctorId(doctor.getId().toString())
            .grantedAt(consent.getGrantedAt())
            .expiresAt(consent.getExpiresAt())
            .build();
        kafkaTemplate.send(CONSENT_TOPIC, patientId.toString(), event);

        return toResponse(consent);
    }

    @Transactional
    public void revokeConsent(UUID consentId, UUID patientId) {
        UserConsent consent = consentRepository.findById(consentId)
            .orElseThrow(() -> new ResourceNotFoundException("Consent", consentId.toString()));
        if (!consent.getPatient().getId().equals(patientId)) {
            throw new UnauthorizedException("Cannot revoke another patient's consent");
        }
        consent.setConsentStatus(ConsentStatus.REVOKED);
        consent.setRevokedAt(Instant.now());
        consentRepository.save(consent);
    }

    public List<ConsentResponse> getConsentsForPatient(UUID patientId) {
        return consentRepository.findByPatientId(patientId).stream()
            .map(this::toResponse)
            .toList();
    }

    private ConsentResponse toResponse(UserConsent c) {
        return new ConsentResponse(
            c.getId(),
            c.getPatient().getId(),
            c.getDoctor().getId(),
            c.getConsentStatus(),
            c.getGrantedAt(),
            c.getExpiresAt(),
            c.getRevokedAt()
        );
    }
}
