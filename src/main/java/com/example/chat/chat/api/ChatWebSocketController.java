package com.example.chat.chat.api;

import java.security.Principal;
import java.time.Instant;
import java.util.Map;

import com.example.chat.config.RealtimeProperties;
import com.example.chat.chat.service.ChatMessageService;
import com.example.chat.chat.service.ConversationService;
import com.example.chat.chat.service.PresenceService;
import com.example.chat.chat.service.RateLimiterService;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.validation.Valid;
import org.springframework.messaging.handler.annotation.MessageExceptionHandler;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.annotation.SendToUser;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Controller;

@Controller
public class ChatWebSocketController {

  private final ChatMessageService messageService;
  private final ConversationService conversationService;
  private final PresenceService presenceService;
  private final RateLimiterService rateLimiterService;
  private final SimpMessagingTemplate messagingTemplate;
  private final RealtimeProperties realtimeProperties;
  private final Counter rejectedCounter;

  public ChatWebSocketController(ChatMessageService messageService,
                                 ConversationService conversationService,
                                 PresenceService presenceService,
                                 RateLimiterService rateLimiterService,
                                 SimpMessagingTemplate messagingTemplate,
                                 RealtimeProperties realtimeProperties,
                                 MeterRegistry meterRegistry) {
    this.messageService = messageService;
    this.conversationService = conversationService;
    this.presenceService = presenceService;
    this.rateLimiterService = rateLimiterService;
    this.messagingTemplate = messagingTemplate;
    this.realtimeProperties = realtimeProperties;
    this.rejectedCounter = Counter.builder("chat.messages.rejected")
        .description("Rejected websocket message sends")
        .register(meterRegistry);
  }

  @MessageMapping("/chat.send")
  public void send(@Valid ChatSendRequest request, Principal principal) {
    if (principal == null) {
      throw new AccessDeniedException("Unauthenticated websocket session");
    }

    String userId = principal.getName();
    if (!rateLimiterService.isAllowed(userId)) {
      rejectedCounter.increment();
      throw new AccessDeniedException("Rate limit exceeded");
    }

    if (request.text().length() > realtimeProperties.getMaxMessageLength()) {
      rejectedCounter.increment();
      throw new IllegalArgumentException("Message exceeds max length");
    }

    conversationService.assertMessagingAllowed(request.conversationId(), userId);
    var persisted = messageService.persistUserMessage(
        request.conversationId(),
        userId,
        request.clientMessageId(),
        request.text());

    presenceService.touch(userId);
    messagingTemplate.convertAndSendToUser(
        userId,
        "/queue/acks",
        new MessageAckResponse(
            persisted.messageId(),
            persisted.clientMessageId(),
            "PERSISTED",
            persisted.createdAt()));
  }

  @MessageMapping("/chat.typing")
  public void typing(@Valid TypingSignal signal, Principal principal) {
    if (principal == null) {
      throw new AccessDeniedException("Unauthenticated websocket session");
    }

    String userId = principal.getName();
    conversationService.assertMessagingAllowed(signal.conversationId(), userId);
    messagingTemplate.convertAndSend(
        "/topic/conversations." + signal.conversationId() + ".typing",
        Map.of(
            "conversationId", signal.conversationId(),
            "userId", userId,
            "typing", signal.typing(),
            "at", Instant.now()));
  }

  @MessageExceptionHandler({AccessDeniedException.class, IllegalArgumentException.class})
  @SendToUser("/queue/errors")
  public ErrorResponse handleKnownException(Exception ex) {
    return new ErrorResponse("BAD_MESSAGE", ex.getMessage());
  }
}
