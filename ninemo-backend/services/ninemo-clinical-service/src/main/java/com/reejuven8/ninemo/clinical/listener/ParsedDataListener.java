package com.reejuven8.ninemo.clinical.listener;

import com.reejuven8.common.event.DocumentParsedEvent;
import com.reejuven8.common.util.CorrelationMdc;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class ParsedDataListener {

    @KafkaListener(topics = "document.data.parsed", groupId = "ninemo-clinical-service")
    public void onDocumentParsed(DocumentParsedEvent event) {
        try (var ignored = CorrelationMdc.restore(event.getCorrelationId())) {
            log.info("Received parsed document event patientId={}", event.getPatientId());
            // Phase 4: parse FHIR observations into symptom/vitals assessments
        }
    }
}
