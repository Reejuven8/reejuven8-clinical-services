package com.reejuven8.ninemo.clinical.publisher;

import com.reejuven8.ninemo.clinical.config.RabbitConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import org.slf4j.MDC;

import java.util.HashMap;
import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class ClinicalRiskPublisher {

    private final RabbitTemplate rabbitTemplate;

    public void publishRiskAlert(String patientId, String riskType, String severityFlag) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("patientId", patientId);
        payload.put("riskType", riskType);
        payload.put("severityFlag", severityFlag);
        payload.put("correlationId", MDC.get("correlationId"));
        rabbitTemplate.convertAndSend(
            RabbitConfig.CLINICAL_RISK_EXCHANGE,
            RabbitConfig.CLINICAL_RISK_ROUTING_KEY,
            payload
        );
        log.info("Published risk alert: patientId={} risk={}", patientId, riskType);
    }
}
