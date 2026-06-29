package com.example.bankcards.dto.person;

import com.example.bankcards.entity.Person;
import com.example.bankcards.entity.Role;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.time.Instant;

/**
 * DTO for {@link Person}
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record PersonDto(Long id, String name, Role role, Instant createdDate, Instant lastModifiedDate) {
}