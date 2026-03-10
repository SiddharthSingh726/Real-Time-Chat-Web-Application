package com.example.chat.chat.api;

import java.time.Instant;
import java.util.List;

public record MessageHistoryResponse(
    List<ChatMessageResponse> items,
    Instant nextCursor
) {
}
