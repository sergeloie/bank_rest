package com.example.bankcards.dto.person;

import com.example.bankcards.entity.Person;
import com.example.bankcards.entity.Role;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * DTO for {@link Person}
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record PersonCreateRequest(@NotBlank(message = "Person name must be specified") String name,
                                  @NotBlank(message = "Person password must be specified") String password,
                                  @NotNull(message = "Person role must be specified") Role role) {
}