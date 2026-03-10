package com.example.chat.chat.api;

import java.time.Instant;
import java.util.UUID;

public record MessageAckResponse(
    UUID messageId,
    String clientMessageId,
    String status,
    Instant persistedAt
) {
}
