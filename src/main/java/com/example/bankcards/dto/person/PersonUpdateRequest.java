package com.example.bankcards.dto.person;

import com.example.bankcards.entity.Role;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Size;

/**
 * Request to update a user's password and/or role. At least one field must be provided.
 *
 * @param password New password. May be null. If provided, must have length >= 1.
 * @param role     New role (USER or ADMIN). May be null.
 */
public record PersonUpdateRequest(
    @Size(min = 1) String password,
    Role role
) {
    @AssertTrue(message = "At least one of 'password' or 'role' must be provided")
    private boolean isAtLeastOneProvided() {
        return password != null || role != null;
    }
}
