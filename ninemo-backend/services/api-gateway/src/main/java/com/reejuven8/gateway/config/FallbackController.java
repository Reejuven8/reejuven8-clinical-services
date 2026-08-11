package com.reejuven8.gateway.config;

import com.reejuven8.common.dto.ApiResponse;
import com.reejuven8.common.dto.ErrorResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class FallbackController {

    @GetMapping("/fallback")
    public ResponseEntity<ApiResponse<Void>> fallback() {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
            .body(ApiResponse.error(ErrorResponse.of("SERVICE_UNAVAILABLE",
                "The service is temporarily unavailable. Please try again later.")));
    }
}
