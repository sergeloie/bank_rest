package com.example.bankcards.dto.cardblockrequest;

import com.example.bankcards.entity.BlockRequestStatus;

import java.time.Instant;
import java.util.UUID;

/**
 * Admin view of a card block request with card and person details.
 *
 * @param id                 Block request ID (Long, auto-generated).
 * @param cardId             UUID of the card to block.
 * @param maskedCardNumber   Masked card number.
 * @param personId           UUID of the person who submitted the request.
 * @param blockRequestStatus Current status (PENDING, APPROVED, REJECTED).
 * @param createdDate        Timestamp when the request was created.
 * @param lastModifiedDate   Timestamp of the last modification.
 * @param createdBy          UUID of the user who created this request.
 * @param modifiedBy         UUID of the user who last modified this request.
 */
public record CardBlockRequestAdminResponse(
    Long id,
    UUID cardId,
    String maskedCardNumber,
    UUID personId,
    BlockRequestStatus blockRequestStatus,
    Instant createdDate,
    Instant lastModifiedDate,
    UUID createdBy,
    UUID modifiedBy
) {
}
