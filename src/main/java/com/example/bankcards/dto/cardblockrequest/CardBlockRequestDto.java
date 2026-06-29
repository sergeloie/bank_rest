package com.example.bankcards.dto.cardblockrequest;

import com.example.bankcards.entity.BlockRequestStatus;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.time.Instant;

@JsonIgnoreProperties(ignoreUnknown = true)
public record CardBlockRequestDto(
    Long id,
    Long cardId,
    Long personId,
    BlockRequestStatus blockRequestStatus,
    Instant createdDate,
    Instant lastModifiedDate
) {
}
