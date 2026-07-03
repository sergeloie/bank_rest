package com.example.bankcards.dto.auth;

import jakarta.validation.constraints.NotBlank;

/**
 * Login request with username and password.
 *
 * @param name     Username. Must not be blank.
 * @param password Password. Must not be blank.
 */
public record AuthRequest(@NotBlank String name, @NotBlank String password) {
}
