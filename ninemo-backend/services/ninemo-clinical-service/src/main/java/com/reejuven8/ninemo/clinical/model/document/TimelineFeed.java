package com.reejuven8.ninemo.clinical.model.document;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.time.Instant;
import java.util.List;
import java.util.Map;

@Document(collection = "ninemo_timeline_feed")
@CompoundIndex(def = "{'pregnancy_profile_id': 1, 'gestational_week': 1}", unique = true)
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class TimelineFeed {

    @Id
    private String id;

    @Field("pregnancy_profile_id")
    private String pregnancyProfileId;

    @Field("gestational_week")
    private int gestationalWeek;

    private int trimester;

    @Field("baby_development")
    private Map<String, Object> babyDevelopment;

    @Field("maternal_changes")
    private List<String> maternalChanges;

    @Field("maternal_symptoms_expected")
    private List<String> maternalSymptomsExpected;

    @Field("scheduled_milestones")
    private List<Map<String, Object>> scheduledMilestones;

    @Field("diet_tips")
    private List<Map<String, Object>> dietTips;

    @Field("yoga_routine")
    private Map<String, Object> yogaRoutine;

    @Field("pinned_reports")
    private List<String> pinnedReports;

    @Field("created_at")
    private Instant createdAt;
}
