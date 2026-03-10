package com.example.chat.chat.api;

import java.time.Instant;
import java.util.UUID;

public record ChatMessageResponse(
    UUID messageId,
    UUID conversationId,
    String senderId,
    String clientMessageId,
    String text,
    Instant createdAt,
    boolean aiGenerated
) {
}
