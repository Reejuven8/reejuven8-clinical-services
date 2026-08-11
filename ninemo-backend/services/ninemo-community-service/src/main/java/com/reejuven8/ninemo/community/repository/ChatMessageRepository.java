package com.reejuven8.ninemo.community.repository;
import com.reejuven8.ninemo.community.model.ChatMessage;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
public interface ChatMessageRepository extends MongoRepository<ChatMessage, String> {
    Page<ChatMessage> findByClubIdAndChannelIdAndIsDeletedFalseOrderBySentAtDesc(
        String clubId, String channelId, Pageable pageable);
}
