package com.example.bankcards.dto.cardblockrequest;

import com.example.bankcards.entity.BlockRequestStatus;

public record CardBlockRequestResponse(
    String maskedCardNumber,
    BlockRequestStatus blockRequestStatus
) {
}
