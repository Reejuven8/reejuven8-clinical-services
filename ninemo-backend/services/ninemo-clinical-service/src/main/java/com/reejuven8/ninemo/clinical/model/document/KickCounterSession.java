package com.reejuven8.ninemo.clinical.model.document;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.time.Instant;
import java.util.List;

@Document(collection = "kick_counter_sessions")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class KickCounterSession {

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

    @Field("total_kicks")
    private int totalKicks;

    @Field("duration_to_10_kicks_minutes")
    private Integer durationTo10KicksMinutes;

    @Field("kick_timestamps")
    private List<Instant> kickTimestamps;

    @Field("is_concerning")
    private boolean isConcerning;

    private String notes;

    @Field("created_at")
    private Instant createdAt;
}
