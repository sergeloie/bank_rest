package com.example.bankcards.dto.cardblockrequest;

import com.example.bankcards.entity.BlockRequestStatus;

public record CardBlockRequestResponse(
    Long id,
    Long cardId,
    Long personId,
    BlockRequestStatus blockRequestStatus
) {
}
