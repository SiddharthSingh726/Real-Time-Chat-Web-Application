package com.example.chat.chat.api;

public record PresenceResponse(
    String userId,
    boolean online
) {
}
