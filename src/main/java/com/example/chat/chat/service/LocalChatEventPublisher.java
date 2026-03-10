package com.example.chat.chat.service;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix = "app.messaging", name = "provider", havingValue = "local", matchIfMissing = true)
public class LocalChatEventPublisher implements ChatEventPublisher {

  @Override
  public void publish(ChatMessageEvent event) {
    // Local mode uses in-process event dispatch and does not require external broker publication.
  }
}
