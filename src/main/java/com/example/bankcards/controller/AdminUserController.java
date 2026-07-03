package com.example.bankcards.controller;

import com.example.bankcards.dto.person.PersonAdminResponse;
import com.example.bankcards.dto.person.PersonCreateRequest;
import com.example.bankcards.dto.person.PersonUpdateRequest;
import com.example.bankcards.service.PersonService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/admin/users")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AdminUserController {
    private final PersonService personService;

    @GetMapping
    public ResponseEntity<Page<PersonAdminResponse>> getAllUsers(Pageable pageable) {
        return ResponseEntity.ok(personService.getAll(pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<PersonAdminResponse> getUserById(@PathVariable UUID id) {
        return ResponseEntity.ok(personService.getById(id));
    }

    @PostMapping
    public ResponseEntity<PersonAdminResponse> createUser(@Valid @RequestBody PersonCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(personService.create(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<PersonAdminResponse> updateUser(@PathVariable UUID id,
                                                          @Valid @RequestBody PersonUpdateRequest request) {
        return ResponseEntity.ok(personService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteUser(@PathVariable UUID id) {
        personService.delete(id);
    }
}
