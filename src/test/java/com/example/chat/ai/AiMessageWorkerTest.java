package com.example.chat.ai;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import com.example.chat.config.AiProperties;
import com.example.chat.chat.service.ChatMessageEvent;
import com.example.chat.chat.service.ChatMessageService;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AiMessageWorkerTest {

  @Mock
  private AiClient aiClient;

  @Mock
  private ChatMessageService chatMessageService;

  private AiProperties properties;
  private AiMessageWorker worker;

  @BeforeEach
  void setUp() {
    properties = new AiProperties();
    properties.setEnabled(true);
    properties.setTriggerPrefix("@ai");
    worker = new AiMessageWorker(properties, aiClient, chatMessageService, new SimpleMeterRegistry());
  }

  @Test
  void triggersAiReplyWhenPrefixMatches() {
    UUID conversationId = UUID.randomUUID();
    ChatMessageEvent event = new ChatMessageEvent(
        UUID.randomUUID(),
        conversationId,
        "user-1",
        "client-1",
        "@ai summarize this",
        Instant.now(),
        false);

    when(aiClient.generateReply(eq(conversationId), eq("summarize this")))
        .thenReturn(Optional.of("summary"));

    worker.onMessage(event);

    verify(chatMessageService).persistAiMessage(conversationId, "summary");
  }

  @Test
  void ignoresNonTriggeredMessages() {
    ChatMessageEvent event = new ChatMessageEvent(
        UUID.randomUUID(),
        UUID.randomUUID(),
        "user-1",
        "client-1",
        "hello",
        Instant.now(),
        false);

    worker.onMessage(event);

    verify(aiClient, never()).generateReply(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.anyString());
    verify(chatMessageService, never()).persistAiMessage(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.anyString());
  }
}
