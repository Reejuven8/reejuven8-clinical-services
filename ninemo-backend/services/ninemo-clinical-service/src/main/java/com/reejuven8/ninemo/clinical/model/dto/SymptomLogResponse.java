package com.reejuven8.ninemo.clinical.model.dto;

import com.reejuven8.ninemo.clinical.model.enums.SeverityFlag;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.List;
import java.util.Map;

@Data @Builder
public class SymptomLogResponse {
    private String id;
    private String patientId;
    private int gestationalWeek;
    private List<Map<String, Object>> symptoms;
    private SeverityFlag severityFlag;
    private List<String> triggeredRules;
    private Instant loggedAt;
}
