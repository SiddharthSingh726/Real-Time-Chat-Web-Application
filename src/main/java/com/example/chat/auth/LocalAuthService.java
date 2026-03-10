package com.example.chat.auth;

import java.time.Duration;
import java.util.List;
import java.util.UUID;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class LocalAuthService {

  public static final String AUTH_TOKEN_HEADER = "X-Auth-Token";

  private static final Duration SESSION_TTL = Duration.ofDays(7);

  private final AppUserRepository userRepository;
  private final PasswordEncoder passwordEncoder;
  private final StringRedisTemplate redisTemplate;

  public LocalAuthService(AppUserRepository userRepository,
                          PasswordEncoder passwordEncoder,
                          StringRedisTemplate redisTemplate) {
    this.userRepository = userRepository;
    this.passwordEncoder = passwordEncoder;
    this.redisTemplate = redisTemplate;
  }

  public AuthResponse register(RegisterRequest request) {
    String userId = normalize(request.userId());
    if (!StringUtils.hasText(userId)) {
      throw new IllegalArgumentException("User ID is required");
    }
    if (userRepository.existsByUserId(userId)) {
      throw new IllegalArgumentException("User ID is already taken");
    }

    AppUser user = new AppUser();
    user.setUserId(userId);
    user.setDisplayName(normalize(request.displayName()));
    user.setPasswordHash(passwordEncoder.encode(request.password()));
    AppUser saved = userRepository.save(user);

    return createAuthResponse(saved);
  }

  public AuthResponse login(LoginRequest request) {
    AppUser user = userRepository.findByUserId(normalize(request.userId()))
        .orElseThrow(() -> new AccessDeniedException("Invalid user ID or password"));

    if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
      throw new AccessDeniedException("Invalid user ID or password");
    }

    return createAuthResponse(user);
  }

  public UserProfileResponse me(String authToken) {
    String userId = resolveUserId(authToken);
    return userRepository.findByUserId(userId)
        .map(user -> new UserProfileResponse(user.getUserId(), user.getDisplayName(), false, false, true))
        .orElseThrow(() -> new AccessDeniedException("Invalid session"));
  }

  public String resolveUserId(String authToken) {
    if (!StringUtils.hasText(authToken)) {
      throw new AccessDeniedException("Missing auth token");
    }
    String userId = redisTemplate.opsForValue().get(sessionKey(authToken));
    if (!StringUtils.hasText(userId)) {
      throw new AccessDeniedException("Invalid session");
    }
    redisTemplate.expire(sessionKey(authToken), SESSION_TTL);
    return userId;
  }

  public boolean isKnownUser(String userId) {
    return userRepository.existsByUserId(normalize(userId));
  }

  public List<UserProfileResponse> listUsers() {
    return userRepository.findAllByOrderByDisplayNameAscUserIdAsc().stream()
        .map(user -> new UserProfileResponse(user.getUserId(), user.getDisplayName(), false, false, true))
        .toList();
  }

  private AuthResponse createAuthResponse(AppUser user) {
    String authToken = UUID.randomUUID().toString();
    redisTemplate.opsForValue().set(sessionKey(authToken), user.getUserId(), SESSION_TTL);
    return new AuthResponse(user.getUserId(), user.getDisplayName(), authToken);
  }

  private String normalize(String value) {
    return value == null ? null : value.trim();
  }

  private String sessionKey(String authToken) {
    return "chat:auth:session:" + authToken;
  }
}
