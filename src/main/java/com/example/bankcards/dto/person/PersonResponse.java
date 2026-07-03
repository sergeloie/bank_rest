package com.example.bankcards.dto.person;

import com.example.bankcards.entity.Person;
import com.example.bankcards.entity.Role;

/**
 * Public user view exposing only name and role.
 *
 * @param name Unique username.
 * @param role User role (USER or ADMIN).
 */
public record PersonResponse(String name, Role role) {
}
