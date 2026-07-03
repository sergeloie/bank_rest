package com.example.bankcards.dto.cardblockrequest;

import com.example.bankcards.entity.BlockRequestStatus;

/**
 * Confirmation returned after submitting a card block request.
 *
 * @param maskedCardNumber   Masked card number of the blocked card.
 * @param blockRequestStatus Current status of the block request (PENDING, APPROVED, REJECTED).
 */
public record CardBlockRequestResponse(
    String maskedCardNumber,
    BlockRequestStatus blockRequestStatus
) {
}
