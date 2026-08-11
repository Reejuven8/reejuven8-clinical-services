package com.reejuven8.ninemo.clinical.model.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data @Builder
public class TimelineResponse {
    private int gestationalWeek;
    private int trimester;
    private Map<String, Object> babyDevelopment;
    private List<String> maternalChanges;
    private List<Map<String, Object>> scheduledMilestones;
    private List<Map<String, Object>> dietTips;
    private Map<String, Object> yogaRoutine;
}
