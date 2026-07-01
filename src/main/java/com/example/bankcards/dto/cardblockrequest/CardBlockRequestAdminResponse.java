package com.example.bankcards.dto.cardblockrequest;

import com.example.bankcards.entity.BlockRequestStatus;

import java.time.Instant;

public record CardBlockRequestAdminResponse(
    Long id,
    Long cardId,
    String maskedCardNumber,
    Long personId,
    BlockRequestStatus blockRequestStatus,
    Instant createdDate,
    Instant lastModifiedDate,
    Long createdBy,
    Long modifiedBy
) {
}
