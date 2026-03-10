package com.example.chat.chat.api;

import java.util.List;

import com.example.chat.chat.domain.ConversationType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateConversationRequest(
    @NotBlank @Size(max = 120) String title,
    @NotNull ConversationType type,
    @NotEmpty List<@NotBlank @Size(max = 120) String> memberIds
) {
}
