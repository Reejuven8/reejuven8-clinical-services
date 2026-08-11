package com.reejuven8.ninemo.clinical.model.document;

import com.reejuven8.ninemo.clinical.model.enums.SeverityFlag;
import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.time.Instant;
import java.util.List;
import java.util.Map;

@Document(collection = "symptom_logs")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class SymptomLog {

    @Id
    private String id;

    @Field("patient_id")
    private String patientId;

    @Field("pregnancy_profile_id")
    private String pregnancyProfileId;

    @Field("gestational_week_at_log")
    private int gestationalWeekAtLog;

    private int trimester;

    private List<Map<String, Object>> symptoms;

    @Field("vitals_at_log")
    private Map<String, Object> vitalsAtLog;

    @Field("severity_flag")
    private SeverityFlag severityFlag;

    @Field("triage_result")
    private Map<String, Object> triageResult;

    @Field("logged_at")
    private Instant loggedAt;

    @Field("created_at")
    private Instant createdAt;
}
