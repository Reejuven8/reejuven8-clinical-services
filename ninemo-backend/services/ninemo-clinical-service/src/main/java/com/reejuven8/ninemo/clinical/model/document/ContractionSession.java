package com.reejuven8.ninemo.clinical.model.document;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.time.Instant;
import java.util.List;
import java.util.Map;

@Document(collection = "contraction_sessions")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ContractionSession {

    @Id
    private String id;

    @Field("patient_id")
    private String patientId;

    @Field("pregnancy_profile_id")
    private String pregnancyProfileId;

    @Field("gestational_week")
    private int gestationalWeek;

    @Field("session_start")
    private Instant sessionStart;

    @Field("session_end")
    private Instant sessionEnd;

    private List<Map<String, Object>> contractions;

    @Field("average_duration_seconds")
    private int averageDurationSeconds;

    @Field("average_interval_seconds")
    private int averageIntervalSeconds;

    @Field("total_contractions")
    private int totalContractions;

    @Field("is_labor_pattern")
    private boolean isLaborPattern;

    @Field("alert_triggered")
    private boolean alertTriggered;

    private String notes;

    @Field("created_at")
    private Instant createdAt;
}
