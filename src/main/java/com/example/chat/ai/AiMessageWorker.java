package com.example.chat.ai;

import com.example.chat.config.AiProperties;
import com.example.chat.chat.service.ChatMessageEvent;
import com.example.chat.chat.service.ChatMessageService;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class AiMessageWorker {

  private final AiProperties aiProperties;
  private final AiClient aiClient;
  private final ChatMessageService chatMessageService;
  private final Counter generatedCounter;

  public AiMessageWorker(AiProperties aiProperties,
                         AiClient aiClient,
                         ChatMessageService chatMessageService,
                         MeterRegistry meterRegistry) {
    this.aiProperties = aiProperties;
    this.aiClient = aiClient;
    this.chatMessageService = chatMessageService;
    this.generatedCounter = Counter.builder("chat.ai.generated_messages").register(meterRegistry);
  }

  @Async("aiExecutor")
  @EventListener
  public void onMessage(ChatMessageEvent event) {
    if (!aiProperties.isEnabled() || event.aiGenerated()) {
      return;
    }

    String triggerPrefix = aiProperties.getTriggerPrefix();
    if (!StringUtils.hasText(triggerPrefix) || !event.text().startsWith(triggerPrefix)) {
      return;
    }

    String prompt = event.text().substring(triggerPrefix.length()).trim();
    if (!StringUtils.hasText(prompt)) {
      return;
    }

    aiClient.generateReply(event.conversationId(), prompt)
        .ifPresent(reply -> {
          chatMessageService.persistAiMessage(event.conversationId(), reply);
          generatedCounter.increment();
        });
  }
}
