package com.example.chat.chat.service;

import java.time.Duration;
import java.util.UUID;

import com.example.chat.chat.repo.ConversationMemberRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Service
public class MembershipService {

  private static final Duration POSITIVE_CACHE_TTL = Duration.ofMinutes(10);
  private static final Duration NEGATIVE_CACHE_TTL = Duration.ofSeconds(30);
  private static final String CACHE_TRUE = "1";
  private static final String CACHE_FALSE = "0";

  private final StringRedisTemplate redis;
  private final ConversationMemberRepository memberRepository;

  public MembershipService(StringRedisTemplate redis, ConversationMemberRepository memberRepository) {
    this.redis = redis;
    this.memberRepository = memberRepository;
  }

  public boolean isMember(UUID conversationId, String userId) {
    String key = membershipKey(conversationId, userId);
    String cached = redis.opsForValue().get(key);
    if (CACHE_TRUE.equals(cached)) {
      return true;
    }
    if (CACHE_FALSE.equals(cached)) {
      return false;
    }

    boolean member = memberRepository.existsByConversation_IdAndUserId(conversationId, userId);
    redis.opsForValue().set(key, member ? CACHE_TRUE : CACHE_FALSE,
        member ? POSITIVE_CACHE_TTL : NEGATIVE_CACHE_TTL);
    return member;
  }

  public void assertMember(UUID conversationId, String userId) {
    if (!isMember(conversationId, userId)) {
      throw new AccessDeniedException("User is not a member of this conversation");
    }
  }

  public void invalidate(UUID conversationId, String userId) {
    redis.delete(membershipKey(conversationId, userId));
  }

  private String membershipKey(UUID conversationId, String userId) {
    return "chat:membership:%s:%s".formatted(conversationId, userId);
  }
}
