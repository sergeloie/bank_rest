package com.example.bankcards.dto.card;

import java.math.BigDecimal;
import java.util.UUID;

public record CardTransferResponse(
    UUID fromCardId,
    UUID toCardId,
    BigDecimal amount,
    BigDecimal newFromBalance,
    BigDecimal newToBalance
) {
}
