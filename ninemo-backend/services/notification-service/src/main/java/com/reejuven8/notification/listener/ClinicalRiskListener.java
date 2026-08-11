package com.reejuven8.notification.listener;

import com.reejuven8.common.util.CorrelationMdc;
import com.reejuven8.notification.model.dto.NotificationRequest;
import com.reejuven8.notification.model.entity.NotificationChannel;
import com.reejuven8.notification.service.NotificationOrchestrator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class ClinicalRiskListener {

    private final NotificationOrchestrator orchestrator;

    @RabbitListener(queues = "#{@clinicalRiskQueue.name}")
    public void onClinicalRisk(Map<String, Object> payload) {
      try (var ignored = CorrelationMdc.restore((String) payload.get("correlationId"))) {
        log.info("Clinical risk alert received: {}", payload);

        String patientId = (String) payload.get("patientId");
        String riskType  = (String) payload.getOrDefault("riskType", "CLINICAL_RISK");
        String severity  = (String) payload.getOrDefault("severityFlag", "WARNING");

        String title = "CRITICAL".equals(severity) ? "⚠️ Urgent Health Alert" : "Health Alert";
        String body  = "A %s risk has been detected: %s. Please consult your doctor immediately."
            .formatted(severity.toLowerCase(), riskType.replace("_", " ").toLowerCase());

        NotificationRequest request = NotificationRequest.builder()
            .userId(UUID.fromString(patientId))
            .eventType("CLINICAL_RISK_" + severity)
            .title(title)
            .body(body)
            .channels(List.of(NotificationChannel.PUSH, NotificationChannel.WHATSAPP))
            .build();

        orchestrator.dispatch(request);
      }
    }
}
