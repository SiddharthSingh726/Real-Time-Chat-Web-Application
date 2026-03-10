package com.example.chat.chat.service;

import java.time.Duration;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Service
public class PresenceService {

  private static final Duration TTL = Duration.ofMinutes(5);

  private final StringRedisTemplate redis;
  private final Map<String, String> sessionUsers = new ConcurrentHashMap<>();

  public PresenceService(StringRedisTemplate redis) {
    this.redis = redis;
  }

  public void onConnected(String sessionId, String userId) {
    sessionUsers.put(sessionId, userId);
    String key = sessionsKey(userId);
    redis.opsForSet().add(key, sessionId);
    redis.expire(key, TTL);
  }

  public void onDisconnected(String sessionId) {
    String userId = sessionUsers.remove(sessionId);
    if (userId == null) {
      return;
    }

    String key = sessionsKey(userId);
    redis.opsForSet().remove(key, sessionId);
    Long size = redis.opsForSet().size(key);
    if (size == null || size <= 0) {
      redis.delete(key);
      return;
    }
    redis.expire(key, TTL);
  }

  public void touch(String userId) {
    redis.expire(sessionsKey(userId), TTL);
  }

  public boolean isOnline(String userId) {
    Long size = redis.opsForSet().size(sessionsKey(userId));
    return size != null && size > 0;
  }

  public Optional<String> findUserBySession(String sessionId) {
    return Optional.ofNullable(sessionUsers.get(sessionId));
  }

  private String sessionsKey(String userId) {
    return "chat:presence:sessions:%s".formatted(userId);
  }
}
