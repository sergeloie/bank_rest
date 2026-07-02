package com.example.bankcards.dto.transfer;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record TransferHistoryResponse(
    Long id,
    UUID personId,
    UUID fromCardId,
    UUID toCardId,
    BigDecimal amount,
    Instant transactionDate
) {}
