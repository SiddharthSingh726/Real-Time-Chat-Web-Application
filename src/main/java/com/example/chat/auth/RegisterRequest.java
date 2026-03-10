package com.example.chat.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
    @NotBlank @Size(max = 120) String userId,
    @NotBlank @Size(max = 120) String displayName,
    @NotBlank @Size(min = 4, max = 120) String password
) {
}
