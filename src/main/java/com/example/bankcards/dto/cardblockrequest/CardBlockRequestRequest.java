package com.example.bankcards.dto.cardblockrequest;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record CardBlockRequestRequest(
    @NotNull(message = "Card id must be specified") UUID cardId,
    @NotNull(message = "Person id must be specified") UUID personId
) {
}
