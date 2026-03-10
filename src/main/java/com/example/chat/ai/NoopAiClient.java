package com.example.chat.ai;

import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Component;

@Component
public class NoopAiClient implements AiClient {

  @Override
  public Optional<String> generateReply(UUID conversationId, String prompt) {
    return Optional.empty();
  }
}
