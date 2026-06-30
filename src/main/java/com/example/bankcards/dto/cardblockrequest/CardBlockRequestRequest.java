package com.example.bankcards.dto.cardblockrequest;

import jakarta.validation.constraints.NotNull;

public record CardBlockRequestRequest(
    @NotNull(message = "Card id must be specified") Long cardId,
    @NotNull(message = "Person id must be specified") Long personId
) {
}
