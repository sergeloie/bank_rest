package com.example.bankcards.dto.card;

import com.example.bankcards.entity.CardStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/**
 * User-facing card view with masked number, status, and balance.
 *
 * @param id             Card UUID.
 * @param maskedNumber   Masked card number (e.g. **** **** **** 7890).
 * @param expirationDate Card expiration date.
 * @param cardStatus     Current card status (ACTIVE, BLOCKED, EXPIRED).
 * @param balance        Current card balance.
 */
public record CardResponse(
    UUID id,
    String maskedNumber,
    LocalDate expirationDate,
    CardStatus cardStatus,
    BigDecimal balance
) {
}
