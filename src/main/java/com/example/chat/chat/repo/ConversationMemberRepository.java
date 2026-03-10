package com.example.chat.chat.repo;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.example.chat.chat.domain.ConversationMember;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ConversationMemberRepository extends JpaRepository<ConversationMember, UUID> {

  boolean existsByConversation_IdAndUserId(UUID conversationId, String userId);

  Optional<ConversationMember> findByConversation_IdAndUserId(UUID conversationId, String userId);

  List<ConversationMember> findByConversation_Id(UUID conversationId);

  List<ConversationMember> findByConversation_IdIn(List<UUID> conversationIds);

  long countByConversation_Id(UUID conversationId);
}
