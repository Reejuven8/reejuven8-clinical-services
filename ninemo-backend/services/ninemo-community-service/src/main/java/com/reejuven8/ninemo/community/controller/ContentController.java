package com.reejuven8.ninemo.community.controller;

import com.reejuven8.common.dto.ApiResponse;
import com.reejuven8.ninemo.community.service.ContentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/ninemo/community")
@RequiredArgsConstructor
@Tag(name = "Content")
public class ContentController {

    private final ContentService contentService;

    @Operation(summary = "List published articles (paginated)")
    @GetMapping("/content")
    public ResponseEntity<ApiResponse<Object>> getContent(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(ApiResponse.success(contentService.listPublished(page, size)));
    }

    @Operation(summary = "List articles by category")
    @GetMapping("/content/category/{category}")
    public ResponseEntity<ApiResponse<Object>> getByCategory(@PathVariable String category) {
        return ResponseEntity.ok(ApiResponse.success(contentService.listByCategory(category)));
    }

    @Operation(summary = "List articles relevant to a specific gestational week")
    @GetMapping("/content/week/{gestationalWeek}")
    public ResponseEntity<ApiResponse<Object>> getByWeek(@PathVariable int gestationalWeek) {
        return ResponseEntity.ok(ApiResponse.success(contentService.listByGestationalWeek(gestationalWeek)));
    }
}
