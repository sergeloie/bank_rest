package com.example.bankcards.dto.person;

import com.example.bankcards.entity.Role;

import java.time.Instant;
import java.util.UUID;

/**
 * Admin view of a user with role, audit metadata, and UUID identifiers.
 *
 * @param id              Person UUID.
 * @param name            Unique username.
 * @param role            User role (USER or ADMIN).
 * @param createdDate     Timestamp when the user was created.
 * @param lastModifiedDate Timestamp of the last modification.
 * @param createdBy       UUID of the user who created this account.
 * @param modifiedBy      UUID of the user who last modified this account.
 */
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
