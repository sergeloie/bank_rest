package com.example.bankcards.dto.auth;

import jakarta.validation.constraints.NotBlank;

public record AuthRequest(
    @NotBlank(message = "Name must be specified") String name,
    @NotBlank(message = "Password must be specified") String password
) {
}
