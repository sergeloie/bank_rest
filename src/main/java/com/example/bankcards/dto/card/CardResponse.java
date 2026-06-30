package com.example.bankcards.dto.card;

import com.example.bankcards.entity.Card;
import com.example.bankcards.entity.CardStatus;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * DTO for {@link Card}
 */
public record CardResponse(Long id, Long personId, String personName, String maskedNumber,
                           LocalDate expirationDate, CardStatus cardStatus,
                           BigDecimal balance) {
}
