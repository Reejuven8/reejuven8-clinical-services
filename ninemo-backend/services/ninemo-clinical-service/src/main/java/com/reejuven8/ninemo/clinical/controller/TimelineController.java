package com.reejuven8.ninemo.clinical.controller;

import com.reejuven8.common.dto.ApiResponse;
import com.reejuven8.ninemo.clinical.model.dto.TimelineResponse;
import com.reejuven8.ninemo.clinical.service.timeline.TimelineService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/ninemo/timeline")
@RequiredArgsConstructor
@Tag(name = "Timeline")
public class TimelineController {

    private final TimelineService timelineService;

    @Operation(summary = "Get pregnancy timeline for the current gestational week")
    @GetMapping("/current")
    public ResponseEntity<ApiResponse<TimelineResponse>> getCurrentWeek(@RequestHeader("X-User-Id") UUID userId) {
        return ResponseEntity.ok(ApiResponse.success(timelineService.getCurrentWeek(userId)));
    }

    @Operation(summary = "Get pregnancy timeline for a specific gestational week")
    @GetMapping("/week/{week}")
    public ResponseEntity<ApiResponse<TimelineResponse>> getWeek(@RequestHeader("X-User-Id") UUID userId,
                                                                 @PathVariable int week) {
        return ResponseEntity.ok(ApiResponse.success(timelineService.getWeek(userId, week)));
    }
}
