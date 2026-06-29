package com.example.bankcards.dto.card;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.math.BigDecimal;

@JsonIgnoreProperties(ignoreUnknown = true)
public record CardTransferResponse(
    Long fromCardId,
    Long toCardId,
    BigDecimal amount,
    BigDecimal newFromBalance,
    BigDecimal newToBalance
) {
}
