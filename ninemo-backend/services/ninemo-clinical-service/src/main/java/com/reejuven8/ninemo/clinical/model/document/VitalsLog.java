package com.reejuven8.ninemo.clinical.model.document;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.time.Instant;
import java.util.Map;

@Document(collection = "vitals_logs")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class VitalsLog {

    @Id
    private String id;

    @Field("patient_id")
    private String patientId;

    @Field("pregnancy_profile_id")
    private String pregnancyProfileId;

    @Field("gestational_week")
    private int gestationalWeek;

    private int trimester;

    @Field("vital_type")
    private String vitalType;

    private Map<String, Object> measurements;

    private String source;

    @Field("device_name")
    private String deviceName;

    @Field("is_within_normal_range")
    private boolean isWithinNormalRange;

    @Field("alert_triggered")
    private boolean alertTriggered;

    @Field("logged_at")
    private Instant loggedAt;

    @Field("created_at")
    private Instant createdAt;
}
