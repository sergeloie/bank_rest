package com.example.bankcards.dto.card;

import com.example.bankcards.entity.Card;
import com.example.bankcards.entity.CardStatus;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

/**
 * DTO for {@link Card}
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record CardResponse(Long id, Long personId, String personName, LocalDate expirationDate, CardStatus cardStatus,
                           BigDecimal balance, Instant createdDate, Instant lastModifiedDate) {
}