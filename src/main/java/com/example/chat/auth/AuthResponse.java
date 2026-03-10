package com.example.chat.auth;

public record AuthResponse(
    String userId,
    String displayName,
    String authToken
) {
}
