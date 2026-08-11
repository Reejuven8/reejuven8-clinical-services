package com.reejuven8.healthdata.controller;

import com.reejuven8.common.dto.ApiResponse;
import com.reejuven8.healthdata.model.dto.FhirResourceResponse;
import com.reejuven8.healthdata.service.FhirResourceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/health/records")
@RequiredArgsConstructor
@Tag(name = "Health Records")
public class HealthRecordController {

    private final FhirResourceService fhirResourceService;

    @Operation(summary = "Paginated list of FHIR resources for the authenticated patient")
    @GetMapping
    public ResponseEntity<ApiResponse<Page<FhirResourceResponse>>> list(
        @RequestHeader("X-User-Id") String userId,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size
    ) {
        return ResponseEntity.ok(ApiResponse.success(fhirResourceService.getByPatient(userId, page, size)));
    }

    @Operation(summary = "Fetch a single FHIR resource by its MongoDB ID")
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<FhirResourceResponse>> getById(@PathVariable String id) {
        return ResponseEntity.ok(ApiResponse.success(fhirResourceService.getById(id)));
    }

    @Operation(summary = "List FHIR resources for a patient, optionally filtered by resourceType")
    @GetMapping("/patient/{patientId}")
    public ResponseEntity<ApiResponse<List<FhirResourceResponse>>> getByPatient(
        @PathVariable String patientId,
        @RequestParam(required = false) String resourceType
    ) {
        List<FhirResourceResponse> records = resourceType != null
            ? fhirResourceService.getByPatientAndType(patientId, resourceType)
            : fhirResourceService.getByPatient(patientId, 0, 100).getContent();
        return ResponseEntity.ok(ApiResponse.success(records));
    }
}
