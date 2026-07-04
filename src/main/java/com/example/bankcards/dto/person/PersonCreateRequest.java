package com.example.bankcards.dto.person;

import com.example.bankcards.entity.Person;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request to create a new user. Role is always set to USER by the server.
 *
 * @param name     Unique username. Must not be blank.
 * @param password User password. Must not be blank. Minimum 8 characters.
 */
public record PersonCreateRequest(@NotBlank(message = "Person name must be specified") String name,
                                  @NotBlank(message = "Person password must be specified")
                                  @Size(min = 8, message = "Password must be at least 8 characters") String password) {
}
