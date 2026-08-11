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
public class MilestoneReminderListener {

    private final NotificationOrchestrator orchestrator;

    @RabbitListener(queues = "#{@milestoneQueue.name}")
    public void onMilestoneDue(Map<String, Object> payload) {
      try (var ignored = CorrelationMdc.restore((String) payload.get("correlationId"))) {
        log.info("Milestone reminder received: {}", payload);

        String patientId    = (String) payload.get("patientId");
        String milestoneName = (String) payload.getOrDefault("milestoneName", "upcoming appointment");
        int week = payload.containsKey("gestationalWeek")
            ? ((Number) payload.get("gestationalWeek")).intValue() : 0;

        String body = "Reminder: %s is due around week %d. Log in to NineMo to prepare."
            .formatted(milestoneName, week);

        NotificationRequest request = NotificationRequest.builder()
            .userId(UUID.fromString(patientId))
            .eventType("PATIENT_MILESTONE_DUE")
            .title("Upcoming Milestone: " + milestoneName)
            .body(body)
            .channels(List.of(NotificationChannel.PUSH, NotificationChannel.WHATSAPP))
            .build();

        orchestrator.dispatch(request);
      }
    }
}
