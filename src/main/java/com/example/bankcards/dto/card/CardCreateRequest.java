package com.example.bankcards.dto.card;

import com.example.bankcards.entity.Card;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * DTO for {@link Card}
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record CardCreateRequest(@NotNull(message = "Person id must be specified") Long personId,
                                @NotNull(message = "Card expiration date must be specified") LocalDate expirationDate,
                                @PositiveOrZero BigDecimal balance) {
}