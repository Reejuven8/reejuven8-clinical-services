package com.reejuven8.healthdata.publisher;

import com.reejuven8.healthdata.config.RabbitConfig;
import com.reejuven8.healthdata.model.dto.DocumentUploadMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class DocumentUploadedPublisher {

    private final RabbitTemplate rabbitTemplate;

    public void publish(DocumentUploadMessage message) {
        rabbitTemplate.convertAndSend(
            RabbitConfig.DOCUMENT_EXCHANGE,
            RabbitConfig.DOCUMENT_ROUTING_KEY,
            message
        );
        log.info("Published document upload patientId={} key={}", message.patientId(), message.s3Key());
    }
}
