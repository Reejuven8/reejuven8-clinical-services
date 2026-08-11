package com.reejuven8.identity.controller;

import com.reejuven8.common.event.AbdmDataReceivedEvent;
import com.reejuven8.identity.service.AbhaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.web.bind.annotation.*;

import org.slf4j.MDC;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/identity/callback")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "ABDM Callbacks")
public class CallbackController {

    private static final String DATA_TOPIC = "abdm.data.received";

    private final AbhaService abhaService;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Operation(summary = "ABDM async consent callback — resolves pending transaction ID")
    @PostMapping("/consent")
    public ResponseEntity<Void> consentCallback(@RequestBody Map<String, Object> payload) {
        String txnId = (String) payload.get("requestId");
        String stored = abhaService.lookupTxnId(txnId);
        if (stored == null) {
            log.warn("Unknown ABDM consent callback txnId={}", txnId);
        } else {
            log.info("ABDM consent callback resolved txnId={}", txnId);
        }
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "ABDM async health data callback — publishes abdm.data.received to Kafka")
    @PostMapping("/data")
    public ResponseEntity<Void> dataCallback(@RequestBody Map<String, Object> payload) {
        String txnId = (String) payload.get("transactionId");
        String patientId = abhaService.lookupTxnId(txnId);

        AbdmDataReceivedEvent event = AbdmDataReceivedEvent.builder()
            .source("identity-abha-service")
            .correlationId(MDC.get("correlationId"))
            .patientId(patientId)
            .build();
        kafkaTemplate.send(DATA_TOPIC, patientId, event);
        return ResponseEntity.ok().build();
    }
}
