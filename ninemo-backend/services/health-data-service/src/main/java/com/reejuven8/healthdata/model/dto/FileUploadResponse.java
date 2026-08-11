package com.reejuven8.healthdata.model.dto;

public record FileUploadResponse(String s3Key, String presignedDownloadUrl, String status) {}
