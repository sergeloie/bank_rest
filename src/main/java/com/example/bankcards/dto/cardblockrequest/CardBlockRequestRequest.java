package com.example.bankcards.dto.cardblockrequest;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

/**
 * Request to submit a card block request for a specific card and person.
 *
 * @param cardId   UUID of the card to block. Must not be null. Card must be ACTIVE and belong to the person.
 * @param personId UUID of the person requesting the block. Must not be null. Must be the card owner.
 */
public record CardBlockRequestRequest(
    @NotNull(message = "Card id must be specified") UUID cardId,
    @NotNull(message = "Person id must be specified") UUID personId
) {
}
