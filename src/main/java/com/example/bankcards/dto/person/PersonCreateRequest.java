package com.example.bankcards.dto.person;

import com.example.bankcards.entity.Person;
import jakarta.validation.constraints.NotBlank;

/**
 * Request to create a new user. Role is always set to USER by the server.
 *
 * @param name     Unique username. Must not be blank.
 * @param password User password. Must not be blank. Minimum length enforced at service level.
 */
public record PersonCreateRequest(@NotBlank(message = "Person name must be specified") String name,
                                  @NotBlank(message = "Person password must be specified") String password) {
}
