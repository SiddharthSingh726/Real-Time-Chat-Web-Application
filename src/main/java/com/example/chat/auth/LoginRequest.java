package com.example.chat.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record LoginRequest(
    @NotBlank @Size(max = 120) String userId,
    @NotBlank @Size(max = 120) String password
) {
}
