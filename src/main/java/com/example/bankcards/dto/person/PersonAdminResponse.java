package com.example.bankcards.dto.person;

import com.example.bankcards.entity.Role;

import java.time.Instant;
import java.util.UUID;

public record PersonAdminResponse(
    UUID id,
    String name,
    Role role,
    Instant createdDate,
    Instant lastModifiedDate,
    UUID createdBy,
    UUID modifiedBy
) {
}
