package com.example.chat.chat.service;

import java.time.Duration;

import com.example.chat.config.RealtimeProperties;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Service
public class RateLimiterService {

  private static final Duration WINDOW = Duration.ofMinutes(1);

  private final StringRedisTemplate redis;
  private final RealtimeProperties realtimeProperties;

  public RateLimiterService(StringRedisTemplate redis, RealtimeProperties realtimeProperties) {
    this.redis = redis;
    this.realtimeProperties = realtimeProperties;
  }

  public boolean isAllowed(String userId) {
    long minuteBucket = System.currentTimeMillis() / 60_000L;
    String key = "chat:rate:%s:%d".formatted(userId, minuteBucket);
    Long count = redis.opsForValue().increment(key);
    if (count != null && count == 1L) {
      redis.expire(key, WINDOW.plusSeconds(5));
    }
    int limit = Math.max(1, realtimeProperties.getRateLimitPerMinute());
    return count != null && count <= limit;
  }
}
