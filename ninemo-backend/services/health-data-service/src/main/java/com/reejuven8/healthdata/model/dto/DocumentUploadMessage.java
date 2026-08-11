package com.reejuven8.healthdata.model.dto;

public record DocumentUploadMessage(
    String patientId,
    String s3Key,
    String contentType,
    String originalFilename,
    String correlationId
) {}
