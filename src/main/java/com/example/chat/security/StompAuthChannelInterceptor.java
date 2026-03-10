package com.example.chat.security;

import java.security.Principal;
import java.util.List;

import com.example.chat.auth.LocalAuthService;
import com.example.chat.config.SecurityProperties;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.lang.Nullable;
import org.springframework.util.StringUtils;

@Component
public class StompAuthChannelInterceptor implements ChannelInterceptor {

  private final SecurityProperties securityProperties;
  private final JwtDecoder jwtDecoder;
  private final LocalAuthService localAuthService;

  public StompAuthChannelInterceptor(SecurityProperties securityProperties,
                                     @Nullable JwtDecoder jwtDecoder,
                                     LocalAuthService localAuthService) {
    this.securityProperties = securityProperties;
    this.jwtDecoder = jwtDecoder;
    this.localAuthService = localAuthService;
  }

  @Override
  public Message<?> preSend(Message<?> message, MessageChannel channel) {
    StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
    if (accessor == null || accessor.getCommand() != StompCommand.CONNECT) {
      return message;
    }

    Principal principal = accessor.getUser();
    if (principal == null) {
      principal = authenticate(accessor);
      accessor.setUser(principal);
    }

    return message;
  }

  private Principal authenticate(StompHeaderAccessor accessor) {
    if (securityProperties.isDevMode()) {
      String userId = resolveUserIdFromToken(accessor);
      if (!StringUtils.hasText(userId) && securityProperties.isAllowHeaderImpersonation()) {
        userId = firstNonBlank(
            accessor.getFirstNativeHeader("x-user-id"),
            accessor.getFirstNativeHeader("user-id"));
      }
      if (!StringUtils.hasText(userId)) {
        throw new AccessDeniedException("Missing auth token for STOMP CONNECT in dev mode");
      }
      return new UsernamePasswordAuthenticationToken(
          userId,
          "N/A",
          List.of(new SimpleGrantedAuthority("ROLE_USER")));
    }

    if (jwtDecoder == null) {
      throw new AccessDeniedException("JWT decoder is unavailable");
    }

    String authorization = accessor.getFirstNativeHeader("Authorization");
    if (!StringUtils.hasText(authorization) || !authorization.startsWith("Bearer ")) {
      throw new AccessDeniedException("Missing bearer token in STOMP Authorization header");
    }

    String token = authorization.substring("Bearer ".length()).trim();
    Jwt jwt = jwtDecoder.decode(token);
    return new JwtAuthenticationToken(jwt);
  }

  private String resolveUserIdFromToken(StompHeaderAccessor accessor) {
    String authToken = accessor.getFirstNativeHeader("x-auth-token");
    if (!StringUtils.hasText(authToken)) {
      return null;
    }
    try {
      return localAuthService.resolveUserId(authToken);
    } catch (Exception ignored) {
      return null;
    }
  }

  private String firstNonBlank(String... values) {
    for (String value : values) {
      if (StringUtils.hasText(value)) {
        return value;
      }
    }
    return null;
  }
}
