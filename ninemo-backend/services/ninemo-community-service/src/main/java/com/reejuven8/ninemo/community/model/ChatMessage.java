package com.reejuven8.ninemo.community.model;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;
import java.time.Instant;
import java.util.List;

@Document(collection = "chat_messages")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ChatMessage {
    @Id private String id;
    @Field("club_id") private String clubId;
    @Field("channel_id") private String channelId;
    @Field("sender_id") private String senderId;
    @Field("sender_alias") private String senderAlias;
    @Field("message_type") @Builder.Default private String messageType = "TEXT";
    @Field("message_body") private String messageBody;
    @Field("reply_to_message_id") private String replyToMessageId;
    @Field("image_url") private String imageUrl;
    @Field("is_deleted") @Builder.Default private Boolean isDeleted = false;
    @Field("reactions") @Builder.Default private List<Object> reactions = List.of();
    @Field("sent_at") @Builder.Default private Instant sentAt = Instant.now();
    @Field("created_at") @Builder.Default private Instant createdAt = Instant.now();
}
