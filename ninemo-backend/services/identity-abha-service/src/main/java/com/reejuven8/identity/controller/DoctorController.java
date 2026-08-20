package com.reejuven8.identity.controller;

import com.reejuven8.common.dto.ApiResponse;
import com.reejuven8.identity.model.dto.DoctorSummaryResponse;
import com.reejuven8.identity.service.DoctorService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/identity/doctors")
@RequiredArgsConstructor
@Tag(name = "Doctors")
public class DoctorController {

    private final DoctorService doctorService;

    @Operation(summary = "Look up a doctor by phone number (used to grant consent without a raw doctor UUID)")
    @GetMapping("/search")
    public ResponseEntity<ApiResponse<DoctorSummaryResponse>> search(@RequestParam String phoneNumber) {
        DoctorSummaryResponse response = doctorService.searchByPhone(phoneNumber);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
