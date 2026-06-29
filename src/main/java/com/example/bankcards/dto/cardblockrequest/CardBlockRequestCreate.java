package com.example.bankcards.dto.cardblockrequest;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.NotNull;

@JsonIgnoreProperties(ignoreUnknown = true)
public record CardBlockRequestCreate(
    @NotNull(message = "Card id must be specified") Long cardId,
    @NotNull(message = "Person id must be specified") Long personId
) {
}
