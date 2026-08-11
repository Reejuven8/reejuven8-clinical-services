package com.reejuven8.ninemo.community.model;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;
import java.time.Instant;
import java.util.List;

@Document(collection = "due_date_clubs")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class DueDateClub {
    @Id private String id;
    @Field("club_name") private String clubName;
    @Field("due_date_month") private String dueDateMonth;
    @Field("members") @Builder.Default private List<Member> members = List.of();
    @Field("member_count") @Builder.Default private Integer memberCount = 0;
    @Field("channels") @Builder.Default private List<Channel> channels = List.of();
    @Field("is_active") @Builder.Default private Boolean isActive = true;
    @Field("created_at") @Builder.Default private Instant createdAt = Instant.now();

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class Member {
        @Field("user_id") private String userId;
        @Field("alias") private String alias;
        @Field("joined_at") @Builder.Default private Instant joinedAt = Instant.now();
    }

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class Channel {
        @Field("channel_id") private String channelId;
        @Field("name") private String name;
        @Field("description") private String description;
        @Field("is_default") @Builder.Default private Boolean isDefault = false;
        @Field("created_at") @Builder.Default private Instant createdAt = Instant.now();
    }
}
