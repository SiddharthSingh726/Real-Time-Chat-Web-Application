package com.example.chat.chat.realtime;

import com.example.chat.chat.api.ChatMessageResponse;
import com.example.chat.chat.service.ChatMessageEvent;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

@Component
public class ChatEventBroadcaster {

  private final SimpMessagingTemplate messagingTemplate;
  private final Counter deliveredCounter;

  public ChatEventBroadcaster(SimpMessagingTemplate messagingTemplate, MeterRegistry meterRegistry) {
    this.messagingTemplate = messagingTemplate;
    this.deliveredCounter = Counter.builder("chat.messages.delivered")
        .description("Total broadcast deliveries dispatched to conversation topics")
        .register(meterRegistry);
  }

  public void broadcast(ChatMessageEvent event) {
    ChatMessageResponse response = new ChatMessageResponse(
        event.messageId(),
        event.conversationId(),
        event.senderId(),
        event.clientMessageId(),
        event.text(),
        event.createdAt(),
        event.aiGenerated());

    messagingTemplate.convertAndSend("/topic/conversations." + event.conversationId(), response);
    deliveredCounter.increment();
  }
}
