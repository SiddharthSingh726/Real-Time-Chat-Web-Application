package com.example.chat.chat.api;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import com.example.chat.chat.domain.ConversationType;

public record ConversationResponse(
    UUID id,
    String title,
    ConversationType type,
    Instant createdAt,
    Instant updatedAt,
    List<String> memberIds,
    boolean admin,
    boolean blocked
) {
}
