package com.reejuven8.ninemo.clinical.model.dto;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.List;
import java.util.Map;

@Data @Builder
public class GrowthMeasurementResponse {
    private String id;
    private String childId;
    private int ageInMonths;
    private Instant measurementDate;
    private double heightCm;
    private double weightKg;
    private Double headCircumferenceCm;
    private Map<String, Double> zScores;
    private Map<String, Integer> percentiles;
    private List<String> alertFlags;
    private int crossedPercentileLines;
}
