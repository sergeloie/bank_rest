package com.example.bankcards.dto.transfer;

import java.math.BigDecimal;
import java.time.Instant;

public record TransferHistoryResponse(
    Long id,
    Long personId,
    Long fromCardId,
    Long toCardId,
    BigDecimal amount,
    Instant transactionDate
) {}
