package com.example.bankcards.dto.auth;

import jakarta.validation.constraints.NotBlank;

/**
 * Request to exchange a refresh token for new access and refresh tokens.
 *
 * @param refreshToken JWT refresh token. Must not be blank.
 */
public record RefreshRequest(@NotBlank String refreshToken) {
}
