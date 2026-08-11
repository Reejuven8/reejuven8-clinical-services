package com.reejuven8.ninemo.clinical.controller;

import com.reejuven8.common.dto.ApiResponse;
import com.reejuven8.ninemo.clinical.model.dto.SummaryCardResponse;
import com.reejuven8.ninemo.clinical.service.SummaryCardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/ninemo/summary-card")
@RequiredArgsConstructor
@Tag(name = "Summary Card")
public class SummaryCardController {

    private final SummaryCardService summaryCardService;

    @Operation(summary = "Get today's health summary card for a patient")
    @GetMapping("/{patientId}")
    public ResponseEntity<ApiResponse<SummaryCardResponse>> getSummaryCard(@PathVariable UUID patientId) {
        return ResponseEntity.ok(ApiResponse.success(summaryCardService.getSummaryCard(patientId)));
    }
}
