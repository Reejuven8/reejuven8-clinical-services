package com.reejuven8.ninemo.clinical.controller;

import com.reejuven8.common.dto.ApiResponse;
import com.reejuven8.ninemo.clinical.model.dto.DietLookupResponse;
import com.reejuven8.ninemo.clinical.service.DietLookupService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/ninemo/diet")
@RequiredArgsConstructor
@Tag(name = "Diet Safety")
public class DietController {

    private final DietLookupService dietLookupService;

    @Operation(summary = "Search food safety ratings for pregnancy diet guidance")
    @GetMapping("/search")
    public ResponseEntity<ApiResponse<List<DietLookupResponse>>> search(@RequestParam String q) {
        return ResponseEntity.ok(ApiResponse.success(dietLookupService.search(q)));
    }
}
