package com.reejuven8.identity.controller;

import com.reejuven8.common.dto.ApiResponse;
import com.reejuven8.identity.model.dto.ConsentRequest;
import com.reejuven8.identity.model.dto.ConsentResponse;
import com.reejuven8.identity.service.ConsentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/identity/consent")
@RequiredArgsConstructor
@Tag(name = "Consent")
public class ConsentController {

    private final ConsentService consentService;

    @Operation(summary = "Grant a doctor access to patient records")
    @PostMapping("/grant")
    public ResponseEntity<ApiResponse<ConsentResponse>> grant(
        @Valid @RequestBody ConsentRequest request,
        @RequestHeader("X-User-Id") String patientId
    ) {
        ConsentResponse response = consentService.grantConsent(UUID.fromString(patientId), request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @Operation(summary = "Revoke a previously granted consent")
    @PostMapping("/revoke/{consentId}")
    public ResponseEntity<ApiResponse<Void>> revoke(
        @PathVariable UUID consentId,
        @RequestHeader("X-User-Id") String patientId
    ) {
        consentService.revokeConsent(consentId, UUID.fromString(patientId));
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @Operation(summary = "List all consents granted by the patient")
    @GetMapping("/list")
    public ResponseEntity<ApiResponse<List<ConsentResponse>>> list(
        @RequestHeader("X-User-Id") String patientId
    ) {
        List<ConsentResponse> consents = consentService.getConsentsForPatient(UUID.fromString(patientId));
        return ResponseEntity.ok(ApiResponse.success(consents));
    }
}
