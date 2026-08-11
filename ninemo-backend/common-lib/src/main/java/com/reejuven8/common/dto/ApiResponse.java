package com.reejuven8.common.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Getter;
import java.time.Instant;
import java.util.UUID;

@Getter
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse<T> {
    private final String status;
    private final T data;
    private final ErrorResponse error;
    @Builder.Default
    private final Metadata metadata = Metadata.defaults();

    public static <T> ApiResponse<T> success(T data) {
        return ApiResponse.<T>builder().status("success").data(data).build();
    }

    public static <T> ApiResponse<T> error(ErrorResponse error) {
        return ApiResponse.<T>builder().status("error").error(error).build();
    }

    @Getter
    @Builder
    public static class Metadata {
        private final Instant timestamp;
        private final String requestId;
        @Builder.Default private final String version = "v1";

        public static Metadata defaults() {
            return Metadata.builder().timestamp(Instant.now()).requestId(UUID.randomUUID().toString()).build();
        }
    }
}
