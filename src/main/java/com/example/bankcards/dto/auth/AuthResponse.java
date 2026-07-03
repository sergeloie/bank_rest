package com.example.bankcards.dto.auth;

/**
 * JWT access and refresh tokens returned after successful authentication.
 *
 * @param accessToken  JWT access token for API authorization.
 * @param refreshToken JWT refresh token for obtaining new access tokens.
 */
public record AuthResponse(String accessToken, String refreshToken) {
}
