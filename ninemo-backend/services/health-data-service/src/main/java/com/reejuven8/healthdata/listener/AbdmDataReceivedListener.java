package com.reejuven8.healthdata.listener;

import com.reejuven8.common.event.AbdmDataReceivedEvent;
import com.reejuven8.common.util.CorrelationMdc;
import com.reejuven8.healthdata.service.RecordSyncService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class AbdmDataReceivedListener {

    private final RecordSyncService recordSyncService;

    @KafkaListener(topics = "abdm.data.received", groupId = "${spring.kafka.consumer.group-id}")
    public void onAbdmDataReceived(AbdmDataReceivedEvent event) {
        try (var ignored = CorrelationMdc.restore(event.getCorrelationId())) {
            log.info("ABDM data received patientId={}", event.getPatientId());
            recordSyncService.processAbdmBundle(event.getPatientId(), event.getFhirBundle());
        }
    }
}
