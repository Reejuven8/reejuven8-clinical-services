package com.reejuven8.ninemo.community.controller;

import com.reejuven8.common.dto.ApiResponse;
import com.reejuven8.ninemo.community.model.dto.JoinClubRequest;
import com.reejuven8.ninemo.community.service.DueDateClubService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/ninemo/community/clubs")
@RequiredArgsConstructor
@Tag(name = "Due Date Clubs")
public class ClubController {

    private final DueDateClubService dueDateClubService;

    @Operation(summary = "Join a due date club matching the patient's due month")
    @PostMapping("/join")
    public ResponseEntity<ApiResponse<Object>> join(
            @RequestHeader("X-User-Id") String userId,
            @Valid @RequestBody JoinClubRequest request) {
        Object club = dueDateClubService.join(userId, request.getDueDateMonth(), request.getAlias());
        return ResponseEntity.ok(ApiResponse.success(club));
    }

    @Operation(summary = "List active clubs the patient belongs to")
    @GetMapping
    public ResponseEntity<ApiResponse<Object>> list(@RequestHeader("X-User-Id") String userId) {
        return ResponseEntity.ok(ApiResponse.success(dueDateClubService.listActive(userId)));
    }

    @Operation(summary = "Get details of a specific club")
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<Object>> getClub(
            @PathVariable String id,
            @RequestHeader("X-User-Id") String userId) {
        return ResponseEntity.ok(ApiResponse.success(dueDateClubService.getClub(id, userId)));
    }

    @Operation(summary = "List channels available in a club")
    @GetMapping("/{id}/channels")
    public ResponseEntity<ApiResponse<Object>> channels(@PathVariable String id) {
        return ResponseEntity.ok(ApiResponse.success(dueDateClubService.getChannels(id)));
    }
}
