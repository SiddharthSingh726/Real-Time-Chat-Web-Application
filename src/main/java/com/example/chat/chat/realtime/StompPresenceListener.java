package com.example.chat.chat.realtime;

import java.security.Principal;

import com.example.chat.chat.service.PresenceService;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionConnectEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

@Component
public class StompPresenceListener {

  private final PresenceService presenceService;

  public StompPresenceListener(PresenceService presenceService) {
    this.presenceService = presenceService;
  }

  @EventListener
  public void onSessionConnect(SessionConnectEvent event) {
    StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
    Principal principal = accessor.getUser();
    if (principal == null) {
      return;
    }

    String sessionId = accessor.getSessionId();
    if (sessionId != null) {
      presenceService.onConnected(sessionId, principal.getName());
    }
  }

  @EventListener
  public void onSessionDisconnect(SessionDisconnectEvent event) {
    String sessionId = event.getSessionId();
    if (sessionId != null) {
      presenceService.onDisconnected(sessionId);
    }
  }
}
