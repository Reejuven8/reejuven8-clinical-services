package com.reejuven8.healthdata.listener;

import com.reejuven8.common.event.ConsentGrantedEvent;
import com.reejuven8.common.util.CorrelationMdc;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class ConsentGrantedListener {

    @KafkaListener(topics = "abdm.consent.granted", groupId = "${spring.kafka.consumer.group-id}")
    public void onConsentGranted(ConsentGrantedEvent event) {
        try (var ignored = CorrelationMdc.restore(event.getCorrelationId())) {
            log.info("Consent granted patientId={} doctorId={} consentId={}",
                event.getPatientId(), event.getDoctorId(), event.getConsentId());
            // Future: trigger ABDM health data pull for consented patient
        }
    }
}
