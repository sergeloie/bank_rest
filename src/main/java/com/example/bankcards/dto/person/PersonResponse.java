package com.example.bankcards.dto.person;

import com.example.bankcards.entity.Person;
import com.example.bankcards.entity.Role;

/**
 * DTO for {@link Person}
 */
public record PersonResponse(Long id, String name, Role role) {
}
