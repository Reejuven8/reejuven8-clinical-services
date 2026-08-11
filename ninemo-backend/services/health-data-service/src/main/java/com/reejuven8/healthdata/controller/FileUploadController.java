package com.reejuven8.healthdata.controller;

import com.reejuven8.common.dto.ApiResponse;
import com.reejuven8.healthdata.model.dto.DocumentUploadMessage;
import com.reejuven8.healthdata.model.dto.FileUploadResponse;
import com.reejuven8.healthdata.publisher.DocumentUploadedPublisher;
import com.reejuven8.healthdata.service.FileStorageService;
import com.reejuven8.healthdata.service.ParseProgressService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.slf4j.MDC;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequestMapping("/api/v1/health/files")
@RequiredArgsConstructor
@Tag(name = "File Storage")
public class FileUploadController {

    private final FileStorageService fileStorageService;
    private final DocumentUploadedPublisher publisher;
    private final ParseProgressService parseProgressService;

    @Operation(summary = "Upload a medical document to S3 and queue for OCR parsing")
    @PostMapping("/upload")
    public ResponseEntity<ApiResponse<FileUploadResponse>> upload(
        @RequestParam("file") MultipartFile file,
        @RequestHeader("X-User-Id") String userId
    ) {
        String s3Key = fileStorageService.upload(file, userId);
        publisher.publish(new DocumentUploadMessage(
            userId, s3Key, file.getContentType(), file.getOriginalFilename(), MDC.get("correlationId")
        ));
        String presignedUrl = fileStorageService.generatePresignedUrl(s3Key);
        FileUploadResponse response = new FileUploadResponse(s3Key, presignedUrl, "PROCESSING");
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @Operation(summary = "Generate a 15-minute presigned S3 URL for a document")
    @GetMapping("/download")
    public ResponseEntity<ApiResponse<String>> download(@RequestParam String s3Key) {
        String url = fileStorageService.generatePresignedUrl(s3Key);
        return ResponseEntity.ok(ApiResponse.success(url));
    }

    @Operation(summary = "SSE stream of parse progress for an uploaded document (PROCESSING → PARSED)")
    @GetMapping(value = "/events", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter parseProgress(@RequestParam String s3Key) {
        return parseProgressService.subscribe(s3Key);
    }
}
