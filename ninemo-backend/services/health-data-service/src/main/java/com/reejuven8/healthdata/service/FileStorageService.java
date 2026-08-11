package com.reejuven8.healthdata.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;

import java.io.IOException;
import java.time.Duration;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class FileStorageService {

    private static final Duration PRESIGN_TTL = Duration.ofMinutes(15);

    private final S3Client s3Client;
    private final S3Presigner s3Presigner;

    @Value("${aws.s3.health-bucket}")
    private String healthBucket;

    public String upload(MultipartFile file, String patientId) {
        String key = "patients/%s/documents/%s/%s".formatted(
            patientId, UUID.randomUUID(), file.getOriginalFilename()
        );
        try {
            s3Client.putObject(
                PutObjectRequest.builder()
                    .bucket(healthBucket)
                    .key(key)
                    .contentType(file.getContentType())
                    .build(),
                RequestBody.fromBytes(file.getBytes())
            );
        } catch (IOException e) {
            throw new IllegalStateException("File upload failed", e);
        }
        log.info("Uploaded patientId={} key={}", patientId, key);
        return key;
    }

    public String generatePresignedUrl(String s3Key) {
        GetObjectPresignRequest request = GetObjectPresignRequest.builder()
            .signatureDuration(PRESIGN_TTL)
            .getObjectRequest(GetObjectRequest.builder()
                .bucket(healthBucket)
                .key(s3Key)
                .build())
            .build();
        return s3Presigner.presignGetObject(request).url().toString();
    }
}
