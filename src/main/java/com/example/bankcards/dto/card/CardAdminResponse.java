package com.example.bankcards.dto.card;

import com.example.bankcards.entity.CardStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Admin view of a card with full owner details, masked number, and audit metadata.
 *
 * @param id              Card UUID.
 * @param personId        UUID of the card owner.
 * @param personName      Name of the card owner.
 * @param maskedNumber    Masked card number (e.g. **** **** **** 7890).
 * @param expirationDate  Card expiration date.
 * @param cardStatus      Current card status (ACTIVE, BLOCKED, EXPIRED).
 * @param balance         Current card balance.
 * @param createdDate     Timestamp when the card was created.
 * @param lastModifiedDate Timestamp of the last modification.
 * @param createdBy       UUID of the user who created this card.
 * @param modifiedBy      UUID of the user who last modified this card.
 */
public record CardAdminResponse(
    UUID id,
    UUID personId,
    String personName,
    String maskedNumber,
    LocalDate expirationDate,
    CardStatus cardStatus,
    BigDecimal balance,
    Instant createdDate,
    Instant lastModifiedDate,
    UUID createdBy,
    UUID modifiedBy
) {
}
