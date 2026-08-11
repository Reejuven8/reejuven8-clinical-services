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
public class MilestoneReminderPublisher {

    private final RabbitTemplate rabbitTemplate;

    public void publishMilestoneReminder(String patientId, String milestoneName, int gestationalWeek) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("patientId", patientId);
        payload.put("milestoneName", milestoneName);
        payload.put("gestationalWeek", gestationalWeek);
        payload.put("correlationId", MDC.get("correlationId"));
        rabbitTemplate.convertAndSend(
            RabbitConfig.MILESTONE_REMINDER_EXCHANGE,
            RabbitConfig.MILESTONE_REMINDER_ROUTING_KEY,
            payload
        );
        log.info("Published milestone reminder: patientId={} milestone={}", patientId, milestoneName);
    }
}
