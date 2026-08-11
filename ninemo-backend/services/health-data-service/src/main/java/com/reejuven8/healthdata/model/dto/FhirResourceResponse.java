package com.reejuven8.healthdata.model.dto;

import java.time.Instant;
import java.util.List;
import java.util.Map;

public record FhirResourceResponse(
    String id,
    String patientId,
    String resourceType,
    String resourceId,
    String status,
    Instant effectiveDatetime,
    String category,
    Map<String, Object> code,
    Map<String, Object> valueQuantity,
    String source,
    String sourceFileS3Url,
    List<String> tags,
    String notes,
    Instant createdAt
) {}
