package com.reejuven8.ninemo.clinical.controller;

import com.reejuven8.common.dto.ApiResponse;
import com.reejuven8.ninemo.clinical.model.dto.ChildProfileResponse;
import com.reejuven8.ninemo.clinical.model.dto.ChildProfileUpdateRequest;
import com.reejuven8.ninemo.clinical.service.pediatric.ModeTransitionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/** X-User-Id scoped; pregnancy ownership is enforced in the service layer (IS-027). */
@RestController
@RequestMapping("/api/v1/ninemo/mode")
@RequiredArgsConstructor
@Tag(name = "Mode Transition")
public class ModeTransitionController {

    private final ModeTransitionService modeTransitionService;

    @Operation(summary = "Transition a pregnancy profile to postnatal (child) mode after delivery. "
                       + "The body is optional — supply it to record the baby's details at transition time.")
    @PostMapping("/transition-to-postnatal/{pregnancyProfileId}")
    public ResponseEntity<ApiResponse<ChildProfileResponse>> transitionToPostnatal(
            @RequestHeader("X-User-Id") UUID userId,
            @PathVariable UUID pregnancyProfileId,
            @Valid @RequestBody(required = false) ChildProfileUpdateRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
            modeTransitionService.transitionToPostnatalMode(pregnancyProfileId, userId, request)));
    }
}
