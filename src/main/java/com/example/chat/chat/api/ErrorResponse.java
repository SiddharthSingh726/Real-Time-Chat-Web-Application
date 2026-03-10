package com.example.chat.chat.api;

public record ErrorResponse(
    String code,
    String message
) {
}
