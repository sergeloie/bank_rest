package com.example.bankcards.dto.person;

import com.example.bankcards.entity.Role;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Size;

public record PersonUpdateRequest(
    @Size(min = 1) String password,
    Role role
) {
    @AssertTrue(message = "At least one of 'password' or 'role' must be provided")
    private boolean isAtLeastOneProvided() {
        return password != null || role != null;
    }
}
