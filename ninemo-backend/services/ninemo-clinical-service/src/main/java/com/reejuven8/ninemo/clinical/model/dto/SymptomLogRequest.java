package com.reejuven8.ninemo.clinical.model.dto;

import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class SymptomLogRequest {
    @NotEmpty
    private List<Map<String, Object>> symptoms;
    private Map<String, Object> vitalsAtLog;
}
