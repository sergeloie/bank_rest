package com.example.bankcards.controller;

import com.example.bankcards.dto.person.PersonCreateRequest;
import com.example.bankcards.dto.person.PersonResponse;
import com.example.bankcards.dto.person.PersonUpdateRequest;
import com.example.bankcards.service.PersonService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/persons")
@RequiredArgsConstructor
public class PersonController {
    private final PersonService personService;

    @GetMapping
    public ResponseEntity<Page<PersonResponse>> getAllPersons(Pageable pageable) {
        return ResponseEntity.ok(personService.getAll(pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<PersonResponse> getPersonById(@PathVariable Long id) {
        return ResponseEntity.ok(personService.getById(id));
    }

    @PostMapping
    public ResponseEntity<PersonResponse> createPerson(@Valid @RequestBody PersonCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(personService.create(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<PersonResponse> updatePerson(@PathVariable Long id,
                                                  @Valid @RequestBody PersonUpdateRequest request) {
        return ResponseEntity.ok(personService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deletePerson(@PathVariable Long id) {
        personService.delete(id);
    }
}
