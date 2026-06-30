package com.example.bankcards.dto.card;

import java.math.BigDecimal;

public record CardTransferResponse(
    Long fromCardId,
    Long toCardId,
    BigDecimal amount,
    BigDecimal newFromBalance,
    BigDecimal newToBalance
) {
}
