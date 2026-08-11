package com.reejuven8.healthdata.listener;

import com.reejuven8.common.event.DocumentParsedEvent;
import com.reejuven8.common.util.CorrelationMdc;
import com.reejuven8.healthdata.service.ParseProgressService;
import com.reejuven8.healthdata.service.RecordSyncService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class ParsedDocumentListener {

    private final RecordSyncService recordSyncService;
    private final ParseProgressService parseProgressService;

    @KafkaListener(topics = "document.data.parsed", groupId = "${spring.kafka.consumer.group-id}")
    public void onDocumentParsed(DocumentParsedEvent event) {
        try (var ignored = CorrelationMdc.restore(event.getCorrelationId())) {
            log.info("Parsed document received patientId={} sourceDocId={} observations={}",
                event.getPatientId(), event.getSourceDocumentId(),
                event.getObservations() == null ? 0 : event.getObservations().size());

            if (event.getObservations() != null) {
                event.getObservations().forEach(obs ->
                    recordSyncService.storeParsedObservation(
                        event.getPatientId(), event.getSourceDocumentId(), obs
                    )
                );
            }
            parseProgressService.notifyParsed(event.getSourceDocumentId());
        }
    }
}
