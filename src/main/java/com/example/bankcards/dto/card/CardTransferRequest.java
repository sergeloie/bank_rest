package com.example.bankcards.dto.card;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Request to transfer funds from one card to another within the same owner.
 *
 * @param fromCardId UUID of the source card. Must not be null.
 * @param toCardId   UUID of the target card. Must not be null. Must differ from fromCardId.
 * @param amount     Transfer amount. Must not be null. Must be positive (> 0).
 */
public record CardTransferRequest(
    @NotNull(message = "Source card id must be specified") UUID fromCardId,
    @NotNull(message = "Target card id must be specified") UUID toCardId,
    @NotNull(message = "Amount must be specified") @Positive(message = "Amount must be positive") BigDecimal amount
) {
}
