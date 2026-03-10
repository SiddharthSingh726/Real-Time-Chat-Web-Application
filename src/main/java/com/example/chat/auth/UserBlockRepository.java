package com.example.chat.auth;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface UserBlockRepository extends JpaRepository<UserBlock, UUID> {

  boolean existsByBlockerIdAndBlockedId(String blockerId, String blockedId);

  Optional<UserBlock> findByBlockerIdAndBlockedId(String blockerId, String blockedId);

  @Query("""
      select ub
      from UserBlock ub
      where ub.blockerId = :userId
        and ub.blockedId in :otherUserIds
      """)
  List<UserBlock> findOutgoingBlocks(String userId, Collection<String> otherUserIds);

  @Query("""
      select ub
      from UserBlock ub
      where ub.blockedId = :userId
        and ub.blockerId in :otherUserIds
      """)
  List<UserBlock> findIncomingBlocks(String userId, Collection<String> otherUserIds);
}
