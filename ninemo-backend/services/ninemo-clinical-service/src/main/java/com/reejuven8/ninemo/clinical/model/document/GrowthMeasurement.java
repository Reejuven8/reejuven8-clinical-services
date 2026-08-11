package com.reejuven8.ninemo.clinical.model.document;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.time.Instant;
import java.util.List;
import java.util.Map;

@Document(collection = "growth_measurements")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class GrowthMeasurement {

    @Id
    private String id;

    @Field("child_id")
    private String childId;

    @Field("age_in_months")
    private int ageInMonths;

    @Field("measurement_date")
    private Instant measurementDate;

    @Field("height_cm")
    private double heightCm;

    @Field("weight_kg")
    private double weightKg;

    @Field("head_circumference_cm")
    private Double headCircumferenceCm;

    @Field("z_scores")
    private Map<String, Double> zScores;

    private Map<String, Integer> percentiles;

    @Field("previous_percentiles")
    private Map<String, Integer> previousPercentiles;

    @Field("alert_flags")
    private List<String> alertFlags;

    @Field("crossed_percentile_lines")
    private int crossedPercentileLines;

    private String notes;

    @Field("created_at")
    private Instant createdAt;
}
