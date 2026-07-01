package com.example.bankcards.dto.auth;

import jakarta.validation.constraints.NotBlank;

public record RefreshRequest(
    @NotBlank(message = "Refresh token must be specified") String refreshToken
) {
}
