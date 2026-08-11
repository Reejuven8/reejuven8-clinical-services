package com.reejuven8.healthdata.integration;

import com.reejuven8.healthdata.config.RabbitConfig;
import com.reejuven8.healthdata.model.document.FhirResource;
import com.reejuven8.healthdata.model.dto.DocumentUploadMessage;
import com.reejuven8.healthdata.publisher.DocumentUploadedPublisher;
import com.reejuven8.healthdata.repository.FhirResourceRepository;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.containers.RabbitMQContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Instant;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * MongoDB CRUD + real RabbitMQ publish/consume round-trip.
 * Skipped automatically when Docker is not available.
 */
@SpringBootTest
@Testcontainers(disabledWithoutDocker = true)
class HealthDataIntegrationTest {

    @Container
    static final MongoDBContainer mongo = new MongoDBContainer("mongo:7");

    @Container
    static final RabbitMQContainer rabbit = new RabbitMQContainer("rabbitmq:3.13-alpine");

    @DynamicPropertySource
    static void props(DynamicPropertyRegistry registry) {
        registry.add("spring.data.mongodb.uri", mongo::getReplicaSetUrl);
        registry.add("spring.rabbitmq.host", rabbit::getHost);
        registry.add("spring.rabbitmq.port", rabbit::getAmqpPort);
        registry.add("spring.rabbitmq.username", rabbit::getAdminUsername);
        registry.add("spring.rabbitmq.password", rabbit::getAdminPassword);
    }

    @Autowired FhirResourceRepository fhirResourceRepository;
    @Autowired DocumentUploadedPublisher publisher;
    @Autowired RabbitTemplate rabbitTemplate;

    @Test
    void fhirResourceCrudRoundTrip() {
        String patientId = "it-patient-1";
        FhirResource saved = fhirResourceRepository.save(FhirResource.builder()
            .patientId(patientId)
            .resourceType("Observation")
            .status("final")
            .effectiveDatetime(Instant.now())
            .code(Map.of("coding", Map.of("code", "718-7", "display", "Haemoglobin")))
            .valueQuantity(Map.of("value", 11.2, "unit", "g/dL"))
            .source("integration-test")
            .build());

        assertThat(saved.getId()).isNotNull();
        FhirResource found = fhirResourceRepository.findById(saved.getId()).orElseThrow();
        assertThat(found.getPatientId()).isEqualTo(patientId);
        assertThat(found.getResourceType()).isEqualTo("Observation");

        fhirResourceRepository.deleteById(saved.getId());
        assertThat(fhirResourceRepository.findById(saved.getId())).isEmpty();
    }

    @Test
    void documentUploadPublishConsumeRoundTrip() {
        DocumentUploadMessage message = new DocumentUploadMessage(
            "it-patient-2",
            "patients/it-patient-2/documents/uuid-1/report.pdf",
            "application/pdf",
            "report.pdf",
            "it-correlation-id"
        );

        publisher.publish(message);

        Object received = rabbitTemplate.receiveAndConvert(RabbitConfig.DOCUMENT_QUEUE, 5_000);
        assertThat(received).isInstanceOf(DocumentUploadMessage.class);
        DocumentUploadMessage round = (DocumentUploadMessage) received;
        assertThat(round.patientId()).isEqualTo("it-patient-2");
        assertThat(round.s3Key()).endsWith("report.pdf");
        assertThat(round.correlationId()).isEqualTo("it-correlation-id");
    }
}
