package com.reejuven8.ninemo.clinical.controller;

import com.reejuven8.common.dto.ApiResponse;
import com.reejuven8.ninemo.clinical.model.document.DevelopmentalMilestone;
import com.reejuven8.ninemo.clinical.service.pediatric.DevelopmentalMilestoneService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/** X-User-Id scoped; child ownership is enforced in the service layer (IS-027). */
@RestController
@RequestMapping("/api/v1/ninemo/milestones")
@RequiredArgsConstructor
@Tag(name = "Developmental Milestones")
public class MilestoneController {

    private final DevelopmentalMilestoneService milestoneService;

    @Operation(summary = "Get all developmental milestone check-ins for a child")
    @GetMapping("/children/{childId}")
    public ResponseEntity<ApiResponse<List<DevelopmentalMilestone>>> getHistory(@RequestHeader("X-User-Id") UUID userId,
                                                                               @PathVariable UUID childId) {
        return ResponseEntity.ok(ApiResponse.success(milestoneService.getHistory(childId, userId)));
    }

    @Operation(summary = "Get or create a WHO milestone check-in for a specific month")
    @GetMapping("/children/{childId}/month/{month}")
    public ResponseEntity<ApiResponse<DevelopmentalMilestone>> getOrCreateCheckIn(@RequestHeader("X-User-Id") UUID userId,
                                                                                 @PathVariable UUID childId,
                                                                                 @PathVariable int month) {
        return ResponseEntity.ok(ApiResponse.success(milestoneService.getOrCreateCheckIn(childId, userId, month)));
    }

    @Operation(summary = "Mark a milestone as achieved or not achieved")
    @PutMapping("/{documentId}/achieve")
    public ResponseEntity<ApiResponse<DevelopmentalMilestone>> markMilestone(@RequestHeader("X-User-Id") UUID userId,
                                                                            @PathVariable String documentId,
                                                                            @RequestParam String milestoneName,
                                                                            @RequestParam boolean achieved) {
        return ResponseEntity.ok(ApiResponse.success(
            milestoneService.markMilestone(documentId, userId, milestoneName, achieved)));
    }
}
