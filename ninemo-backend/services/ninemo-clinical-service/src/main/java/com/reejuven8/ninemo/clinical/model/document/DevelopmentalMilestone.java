package com.reejuven8.ninemo.clinical.model.document;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.time.Instant;
import java.util.List;
import java.util.Map;

@Document(collection = "developmental_milestones")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class DevelopmentalMilestone {

    @Id
    private String id;

    @Field("child_id")
    private String childId;

    private int month;

    private String category;

    private List<Map<String, Object>> milestones;

    @Field("alert_flags")
    private List<String> alertFlags;

    @Field("reviewed_at")
    private Instant reviewedAt;

    @Field("created_at")
    private Instant createdAt;

    @Field("updated_at")
    private Instant updatedAt;
}
