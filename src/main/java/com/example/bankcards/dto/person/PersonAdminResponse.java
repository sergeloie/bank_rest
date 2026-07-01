package com.example.bankcards.dto.person;

import com.example.bankcards.entity.Role;

import java.time.Instant;

public record PersonAdminResponse(
    Long id,
    String name,
    Role role,
    Instant createdDate,
    Instant lastModifiedDate,
    Long createdBy,
    Long modifiedBy
) {
}
