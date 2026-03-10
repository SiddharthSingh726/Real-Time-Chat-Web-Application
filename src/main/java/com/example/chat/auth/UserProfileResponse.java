package com.example.chat.auth;

public record UserProfileResponse(
    String userId,
    String displayName,
    boolean blockedByMe,
    boolean hasBlockedMe,
    boolean directChatAllowed
) {
}
