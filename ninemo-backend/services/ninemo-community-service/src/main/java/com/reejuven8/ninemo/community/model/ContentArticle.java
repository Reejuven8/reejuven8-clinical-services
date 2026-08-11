package com.reejuven8.ninemo.community.model;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.time.Instant;
import java.util.List;

@Document(collection = "content_articles")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ContentArticle {
    @Id private String id;

    @Field("title") private String title;
    @Field("body") private String body;
    @Field("summary") private String summary;
    @Field("category") @Indexed private String category;

    @Field("tags") @Builder.Default private List<String> tags = List.of();
    @Field("gestational_weeks") @Builder.Default private List<Integer> gestationalWeeks = List.of();
    @Field("is_published") @Builder.Default private Boolean isPublished = true;

    @Field("author") private String author;
    @Field("image_url") private String imageUrl;
    @Field("published_at") @Builder.Default private Instant publishedAt = Instant.now();
    @Field("created_at") @Builder.Default private Instant createdAt = Instant.now();
}
