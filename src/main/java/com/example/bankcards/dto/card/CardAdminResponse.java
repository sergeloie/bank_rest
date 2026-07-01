package com.example.bankcards.dto.card;

import com.example.bankcards.entity.CardStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

public record CardAdminResponse(
    Long id,
    Long personId,
    String personName,
    String maskedNumber,
    LocalDate expirationDate,
    CardStatus cardStatus,
    BigDecimal balance,
    Instant createdDate,
    Instant lastModifiedDate,
    Long createdBy,
    Long modifiedBy
) {
}
