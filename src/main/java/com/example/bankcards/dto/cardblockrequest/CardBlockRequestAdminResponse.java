package com.example.bankcards.dto.cardblockrequest;

import com.example.bankcards.entity.BlockRequestStatus;

import java.time.Instant;
import java.util.UUID;

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
