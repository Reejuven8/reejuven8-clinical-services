package com.reejuven8.common.dto;

import lombok.Builder;
import lombok.Getter;
import java.util.List;

@Getter
@Builder
public class ErrorResponse {
    private final String code;
    private final String message;
    @Builder.Default
    private final List<String> details = List.of();

    public static ErrorResponse of(String code, String message) {
        return ErrorResponse.builder().code(code).message(message).build();
    }
}
