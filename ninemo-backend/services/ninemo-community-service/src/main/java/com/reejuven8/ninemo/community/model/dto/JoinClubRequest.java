package com.reejuven8.ninemo.community.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class JoinClubRequest {
    @NotBlank
    @Pattern(regexp = "\\d{4}-\\d{2}", message = "dueDateMonth must be YYYY-MM format")
    private String dueDateMonth;

    @NotBlank
    private String alias;
}
