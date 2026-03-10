package com.example.chat.chat.repo;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.example.chat.chat.domain.Conversation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface ConversationRepository extends JpaRepository<Conversation, UUID> {

  @Query("""
      select distinct c
      from Conversation c
      join ConversationMember m on m.conversation = c
      where m.userId = :userId
      order by c.updatedAt desc
      """)
  List<Conversation> findAllForUserOrdered(String userId);

  @Query("""
      select c
      from Conversation c
      join ConversationMember m on m.conversation = c
      where c.type = com.example.chat.chat.domain.ConversationType.DIRECT
        and m.userId in :userIds
      group by c
      having count(m.id) = 2 and count(distinct m.userId) = 2
      """)
  Optional<Conversation> findExistingDirectConversation(Collection<String> userIds);
}
