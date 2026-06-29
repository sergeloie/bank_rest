package com.example.bankcards.dto.card;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

@JsonIgnoreProperties(ignoreUnknown = true)
public record CardTransferRequest(
    @NotNull(message = "Source card id must be specified") Long fromCardId,
    @NotNull(message = "Target card id must be specified") Long toCardId,
    @NotNull(message = "Amount must be specified") @Positive(message = "Amount must be positive") BigDecimal amount,
    @NotNull(message = "Person id must be specified") Long personId
) {
}
