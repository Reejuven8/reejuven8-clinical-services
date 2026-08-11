package com.reejuven8.ninemo.clinical.model.dto;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.List;
import java.util.Map;

@Data @Builder
public class ContractionSessionResponse {
    private String sessionId;
    private String patientId;
    private int gestationalWeek;
    private Instant sessionStart;
    private Instant sessionEnd;
    private int totalContractions;
    private int averageDurationSeconds;
    private int averageIntervalSeconds;
    private boolean isLaborPattern;
    private boolean alertTriggered;
    private List<Map<String, Object>> contractions;
}
