package com.example.chat.chat.service;

import java.time.Instant;
import java.util.UUID;

public record ChatMessageEvent(
    UUID messageId,
    UUID conversationId,
    String senderId,
    String clientMessageId,
    String text,
    Instant createdAt,
    boolean aiGenerated
) {
}
