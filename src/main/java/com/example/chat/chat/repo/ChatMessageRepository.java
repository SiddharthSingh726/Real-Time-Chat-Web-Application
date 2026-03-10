package com.example.chat.chat.repo;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.example.chat.chat.domain.ChatMessage;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, UUID> {

  Optional<ChatMessage> findByConversation_IdAndSenderIdAndClientMessageId(
      UUID conversationId,
      String senderId,
      String clientMessageId);

  List<ChatMessage> findByConversation_IdAndCreatedAtLessThanOrderByCreatedAtDesc(
      UUID conversationId,
      Instant before,
      Pageable pageable);

  List<ChatMessage> findByConversation_IdOrderByCreatedAtDesc(UUID conversationId, Pageable pageable);

  void deleteByConversation_Id(UUID conversationId);
}
