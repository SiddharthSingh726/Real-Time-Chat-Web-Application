package com.example.chat.auth;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class UserBlockService {

  private final UserBlockRepository userBlockRepository;
  private final AppUserRepository appUserRepository;

  public UserBlockService(UserBlockRepository userBlockRepository,
                          AppUserRepository appUserRepository) {
    this.userBlockRepository = userBlockRepository;
    this.appUserRepository = appUserRepository;
  }

  @Transactional
  public void block(String blockerId, String blockedId) {
    String normalizedBlockedId = normalize(blockedId);
    if (!StringUtils.hasText(normalizedBlockedId)) {
      throw new IllegalArgumentException("User ID is required");
    }
    if (blockerId.equals(normalizedBlockedId)) {
      throw new IllegalArgumentException("You cannot block yourself");
    }
    if (!appUserRepository.existsByUserId(normalizedBlockedId)) {
      throw new IllegalArgumentException("Unknown user ID: " + normalizedBlockedId);
    }
    if (userBlockRepository.existsByBlockerIdAndBlockedId(blockerId, normalizedBlockedId)) {
      return;
    }

    UserBlock block = new UserBlock();
    block.setBlockerId(blockerId);
    block.setBlockedId(normalizedBlockedId);
    userBlockRepository.save(block);
  }

  @Transactional
  public void unblock(String blockerId, String blockedId) {
    userBlockRepository.findByBlockerIdAndBlockedId(blockerId, normalize(blockedId))
        .ifPresent(userBlockRepository::delete);
  }

  public void assertDirectConversationAllowed(String actorUserId, String otherUserId) {
    UserBlockState state = getState(actorUserId, otherUserId);
    if (!state.directChatAllowed()) {
      throw new AccessDeniedException("Direct chat is unavailable because one user blocked the other");
    }
  }

  public UserBlockState getState(String actorUserId, String otherUserId) {
    boolean blockedByMe = userBlockRepository.existsByBlockerIdAndBlockedId(actorUserId, otherUserId);
    boolean hasBlockedMe = userBlockRepository.existsByBlockerIdAndBlockedId(otherUserId, actorUserId);
    return new UserBlockState(blockedByMe, hasBlockedMe);
  }

  public Map<String, UserBlockState> getStates(String actorUserId, Collection<String> otherUserIds) {
    Set<String> filteredIds = new HashSet<>();
    for (String otherUserId : otherUserIds) {
      if (StringUtils.hasText(otherUserId) && !actorUserId.equals(otherUserId)) {
        filteredIds.add(otherUserId);
      }
    }

    Map<String, UserBlockState> states = new HashMap<>();
    for (String otherUserId : filteredIds) {
      states.put(otherUserId, new UserBlockState(false, false));
    }
    if (filteredIds.isEmpty()) {
      return states;
    }

    for (UserBlock block : userBlockRepository.findOutgoingBlocks(actorUserId, filteredIds)) {
      states.put(block.getBlockedId(), new UserBlockState(true, states.get(block.getBlockedId()).hasBlockedMe()));
    }
    for (UserBlock block : userBlockRepository.findIncomingBlocks(actorUserId, filteredIds)) {
      states.put(block.getBlockerId(), new UserBlockState(states.get(block.getBlockerId()).blockedByMe(), true));
    }
    return states;
  }

  private String normalize(String value) {
    return value == null ? null : value.trim();
  }
}
