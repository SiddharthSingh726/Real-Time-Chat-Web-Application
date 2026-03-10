package com.example.chat.ai;

import java.util.Optional;
import java.util.UUID;

public interface AiClient {

  Optional<String> generateReply(UUID conversationId, String prompt);
}
