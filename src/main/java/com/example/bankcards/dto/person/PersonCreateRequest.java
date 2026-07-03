package com.example.bankcards.dto.person;

import com.example.bankcards.entity.Person;
import jakarta.validation.constraints.NotBlank;

/**
 * DTO for {@link Person}
 */
public record PersonCreateRequest(@NotBlank(message = "Person name must be specified") String name,
                                  @NotBlank(message = "Person password must be specified") String password) {
}
