package com.example.bankcards.dto.person;

import com.example.bankcards.entity.Role;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@JsonIgnoreProperties(ignoreUnknown = true)
public record PersonUpdateRequest(
    @NotBlank(message = "Person name must be specified") String name,
    @NotBlank(message = "Person password must be specified") String password,
    @NotNull(message = "Person role must be specified") Role role
) {
}
