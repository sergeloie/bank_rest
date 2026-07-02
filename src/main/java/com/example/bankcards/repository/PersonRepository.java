package com.example.bankcards.repository;

import com.example.bankcards.entity.Person;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface PersonRepository extends JpaRepository<Person, UUID> {
    Optional<Person> findByName(String name);

    boolean existsByName(String name);
}
