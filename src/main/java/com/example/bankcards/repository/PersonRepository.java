package com.example.bankcards.repository;

import com.example.bankcards.entity.Person;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PersonRepository extends JpaRepository<Person, Long> {
    Optional<Person> findByName(String name);

    boolean existsByName(String name);
}