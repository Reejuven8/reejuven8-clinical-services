package com.reejuven8.ninemo.clinical.controller;

import com.reejuven8.common.dto.ApiResponse;
import com.reejuven8.ninemo.clinical.model.dto.ChildProfileResponse;
import com.reejuven8.ninemo.clinical.model.dto.ChildProfileUpdateRequest;
import com.reejuven8.ninemo.clinical.service.pediatric.ChildProfileService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * NM-B-169 / IS-028. All routes are X-User-Id scoped; {childId} is ownership-checked.
 */
@RestController
@RequestMapping("/api/v1/ninemo/children")
@RequiredArgsConstructor
@Tag(name = "Child Profile")
public class ChildProfileController {

    private final ChildProfileService childProfileService;

    @Operation(summary = "List the caller's active children")
    @GetMapping
    public ResponseEntity<ApiResponse<List<ChildProfileResponse>>> list(
            @RequestHeader("X-User-Id") UUID userId) {
        return ResponseEntity.ok(ApiResponse.success(childProfileService.listForParent(userId)));
    }

    @Operation(summary = "Get one of the caller's children")
    @GetMapping("/{childId}")
    public ResponseEntity<ApiResponse<ChildProfileResponse>> get(
            @RequestHeader("X-User-Id") UUID userId,
            @PathVariable UUID childId) {
        return ResponseEntity.ok(ApiResponse.success(childProfileService.get(childId, userId)));
    }

    @Operation(summary = "Partially update a child (name, sex, birth measurements, blood group)")
    @PatchMapping("/{childId}")
    public ResponseEntity<ApiResponse<ChildProfileResponse>> update(
            @RequestHeader("X-User-Id") UUID userId,
            @PathVariable UUID childId,
            @Valid @RequestBody ChildProfileUpdateRequest request) {
        return ResponseEntity.ok(ApiResponse.success(childProfileService.update(childId, userId, request)));
    }
}
