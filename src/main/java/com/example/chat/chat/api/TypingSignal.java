package com.example.chat.chat.api;

import java.util.UUID;

import jakarta.validation.constraints.NotNull;

public record TypingSignal(
    @NotNull UUID conversationId,
    boolean typing
) {
}
