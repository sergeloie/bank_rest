package com.example.bankcards.dto.card;

import com.example.bankcards.entity.Card;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/**
 * DTO for {@link Card}
 */
public record CardCreateRequest(@NotNull(message = "Person id must be specified") UUID personId,
                                @NotNull(message = "Card expiration date must be specified")
                                @Future(message = "Card expiration date must be in the future") LocalDate expirationDate,
                                @PositiveOrZero BigDecimal balance) {
}
