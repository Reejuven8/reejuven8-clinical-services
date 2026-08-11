package com.reejuven8.ninemo.clinical.controller;

import com.reejuven8.common.dto.ApiResponse;
import com.reejuven8.ninemo.clinical.model.entity.VaccinationRecord;
import com.reejuven8.ninemo.clinical.service.pediatric.VaccinationScheduleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/** X-User-Id scoped; child ownership is enforced in the service layer (IS-027). */
@RestController
@RequestMapping("/api/v1/ninemo/vaccinations")
@RequiredArgsConstructor
@Tag(name = "Vaccinations")
public class VaccinationController {

    private final VaccinationScheduleService vaccinationScheduleService;

    @Operation(summary = "Generate IAP vaccination schedule for a child")
    @GetMapping("/children/{childId}/schedule")
    public ResponseEntity<ApiResponse<List<VaccinationRecord>>> getSchedule(@RequestHeader("X-User-Id") UUID userId,
                                                                           @PathVariable UUID childId) {
        return ResponseEntity.ok(ApiResponse.success(vaccinationScheduleService.getSchedule(childId, userId)));
    }

    @Operation(summary = "Mark a vaccination as administered")
    @PutMapping("/{vaccinationId}/mark-completed")
    public ResponseEntity<ApiResponse<VaccinationRecord>> markCompleted(
            @RequestHeader("X-User-Id") UUID userId,
            @PathVariable UUID vaccinationId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate administeredDate,
            @RequestParam(required = false) String administeredBy) {
        return ResponseEntity.ok(ApiResponse.success(
            vaccinationScheduleService.markCompleted(vaccinationId, userId, administeredDate, administeredBy)));
    }
}
