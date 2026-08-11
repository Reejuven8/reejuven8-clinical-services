package com.reejuven8.ninemo.clinical.controller;

import com.reejuven8.common.dto.ApiResponse;
import com.reejuven8.ninemo.clinical.model.document.GrowthMeasurement;
import com.reejuven8.ninemo.clinical.model.dto.GrowthInputRequest;
import com.reejuven8.ninemo.clinical.service.pediatric.GrowthChartService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/** X-User-Id scoped; {childId} ownership is enforced in the service layer (IS-027). */
@RestController
@RequestMapping("/api/v1/ninemo/growth")
@RequiredArgsConstructor
@Tag(name = "Growth Chart")
public class GrowthController {

    private final GrowthChartService growthChartService;

    @Operation(summary = "Record a child growth measurement and compute WHO Z-scores")
    @PostMapping("/children/{childId}/measurements")
    public ResponseEntity<ApiResponse<GrowthMeasurement>> recordMeasurement(@RequestHeader("X-User-Id") UUID userId,
                                                                            @PathVariable UUID childId,
                                                                            @Valid @RequestBody GrowthInputRequest request) {
        return ResponseEntity.ok(ApiResponse.success(growthChartService.recordMeasurement(childId, userId, request)));
    }

    @Operation(summary = "Get all growth measurements for a child")
    @GetMapping("/children/{childId}/measurements")
    public ResponseEntity<ApiResponse<List<GrowthMeasurement>>> getHistory(@RequestHeader("X-User-Id") UUID userId,
                                                                          @PathVariable UUID childId) {
        return ResponseEntity.ok(ApiResponse.success(growthChartService.getHistory(childId, userId)));
    }
}
