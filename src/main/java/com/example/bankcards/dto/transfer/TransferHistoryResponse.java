package com.example.bankcards.dto.transfer;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Admin view of a transfer record with source, target, amount, and timestamp.
 *
 * @param id              Transfer history ID (Long, auto-generated).
 * @param personId        UUID of the person who performed the transfer.
 * @param fromCardId      UUID of the source card.
 * @param toCardId        UUID of the target card.
 * @param amount          Transferred amount.
 * @param transactionDate Timestamp when the transfer occurred.
 */
public record TransferHistoryResponse(
    Long id,
    UUID personId,
    UUID fromCardId,
    UUID toCardId,
    BigDecimal amount,
    Instant transactionDate
) {}
