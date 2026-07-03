package com.example.bankcards.dto.card;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Result of a successful card-to-card transfer with updated balances.
 *
 * @param fromCardId    UUID of the source card.
 * @param toCardId      UUID of the target card.
 * @param amount        Transferred amount.
 * @param newFromBalance Updated balance of the source card after transfer.
 * @param newToBalance  Updated balance of the target card after transfer.
 */
public record CardTransferResponse(
    UUID fromCardId,
    UUID toCardId,
    BigDecimal amount,
    BigDecimal newFromBalance,
    BigDecimal newToBalance
) {
}
