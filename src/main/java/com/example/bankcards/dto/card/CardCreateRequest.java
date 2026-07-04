package com.example.bankcards.dto.card;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Request to create a new card for a person with an expiration date and optional balance.
 *
 * @param personId       UUID of the card owner. Must not be null.
 * @param expirationDate Card expiration date. Must not be null. Must be at least tomorrow.
 * @param balance        Initial card balance. May be null (defaults to zero). Must be >= 0.
 */
public record CardCreateRequest(@NotNull(message = "Person id must be specified") UUID personId,
                                @NotNull(message = "Card expiration date must be specified")
                                @Future(message = "Card expiration date must be at least tomorrow")
                                LocalDate expirationDate,
                                @PositiveOrZero BigDecimal balance) {
}
