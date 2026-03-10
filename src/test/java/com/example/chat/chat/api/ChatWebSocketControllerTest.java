package com.example.chat.chat.api;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.security.Principal;
import java.time.Instant;
import java.util.UUID;

import com.example.chat.config.RealtimeProperties;
import com.example.chat.chat.service.ChatMessageService;
import com.example.chat.chat.service.ConversationService;
import com.example.chat.chat.service.PresenceService;
import com.example.chat.chat.service.RateLimiterService;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;

@ExtendWith(MockitoExtension.class)
class ChatWebSocketControllerTest {

  @Mock
  private ChatMessageService messageService;

  @Mock
  private ConversationService conversationService;

  @Mock
  private PresenceService presenceService;

  @Mock
  private RateLimiterService rateLimiterService;

  @Mock
  private SimpMessagingTemplate messagingTemplate;

  @Mock
  private Principal principal;

  private ChatWebSocketController controller;

  @BeforeEach
  void setUp() {
    RealtimeProperties realtimeProperties = new RealtimeProperties();
    realtimeProperties.setMaxMessageLength(4000);
    controller = new ChatWebSocketController(
        messageService,
        conversationService,
        presenceService,
        rateLimiterService,
        messagingTemplate,
        realtimeProperties,
        new SimpleMeterRegistry());
  }

  @Test
  void sendsAckAfterPersistingMessage() {
    UUID conversationId = UUID.randomUUID();
    ChatSendRequest request = new ChatSendRequest(conversationId, "client-1", "hello");
    ChatMessageResponse persisted = new ChatMessageResponse(
        UUID.randomUUID(),
        conversationId,
        "user-1",
        "client-1",
        "hello",
        Instant.now(),
        false);

    when(principal.getName()).thenReturn("user-1");
    when(rateLimiterService.isAllowed("user-1")).thenReturn(true);
    when(messageService.persistUserMessage(conversationId, "user-1", "client-1", "hello"))
        .thenReturn(persisted);

    controller.send(request, principal);

    verify(conversationService).assertMessagingAllowed(conversationId, "user-1");
    verify(messageService).persistUserMessage(conversationId, "user-1", "client-1", "hello");
    verify(messagingTemplate).convertAndSendToUser(eq("user-1"), eq("/queue/acks"), any(MessageAckResponse.class));
    verify(presenceService).touch("user-1");
  }
}
