package com.example.chat.chat.api;

import java.util.UUID;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ChatSendRequest(
    @NotNull UUID conversationId,
    @NotBlank @Size(max = 120) String clientMessageId,
    @NotBlank @Size(max = 4000) String text
) {
}
