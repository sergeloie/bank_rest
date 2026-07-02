package com.example.bankcards.dto.card;

import com.example.bankcards.entity.CardStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record CardResponse(
    UUID id,
    String maskedNumber,
    LocalDate expirationDate,
    CardStatus cardStatus,
    BigDecimal balance
) {
}
