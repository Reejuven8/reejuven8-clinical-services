package com.reejuven8.ninemo.clinical.model.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Data @Builder
public class SummaryCardResponse {
    private String patientId;
    private String patientName;
    private String abhaAddress;
    private int gestationalWeek;
    private int trimester;
    private LocalDate eddDate;
    private String bloodGroup;
    private List<String> highRiskFlags;
    private Map<String, Object> latestVitals;
    private List<Map<String, Object>> recentSymptoms;
    private List<Map<String, Object>> pendingMilestones;
}
