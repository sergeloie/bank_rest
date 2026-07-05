package com.example.bankcards.controller;

import com.example.bankcards.dto.person.PersonAdminResponse;
import com.example.bankcards.dto.person.PersonCreateRequest;
import com.example.bankcards.dto.person.PersonUpdateRequest;
import com.example.bankcards.service.PersonService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/admin/users")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
@Tag(name = "Admin Users", description = "Admin endpoints for managing users")
@SecurityRequirement(name = "bearerAuth")
public class AdminUserController {
    private final PersonService personService;

    @Operation(summary = "Get all users with pagination",
            description = "Returns paginated list of all users with their roles and audit metadata")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Users retrieved successfully",
                    content = @Content(schema = @Schema(implementation = Page.class))),
            @ApiResponse(responseCode = "403", description = "Access denied - admin role required",
                    content = @Content)
    })
    @GetMapping
    public ResponseEntity<Page<PersonAdminResponse>> getAllUsers(Pageable pageable) {
        return ResponseEntity.ok(personService.getAll(pageable));
    }

    @Operation(summary = "Get user by ID",
            description = "Returns full user details including role, creation/modification timestamps, and audit fields")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "User found",
                    content = @Content(schema = @Schema(implementation = PersonAdminResponse.class))),
            @ApiResponse(responseCode = "403", description = "Access denied - admin role required",
                    content = @Content),
            @ApiResponse(responseCode = "404", description = "User not found",
                    content = @Content)
    })
    @GetMapping("/{id}")
    public ResponseEntity<PersonAdminResponse> getUserById(
            @Parameter(description = "User UUID", required = true)
            @PathVariable UUID id) {
        return ResponseEntity.ok(personService.getById(id));
    }

    @Operation(summary = "Create new user",
            description = "Creates a new user with USER role. Password is hashed with BCrypt. " +
                    "Username must be unique. Password minimum 8 characters.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "User created successfully",
                    content = @Content(schema = @Schema(implementation = PersonAdminResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid request (duplicate username, password too short)",
                    content = @Content),
            @ApiResponse(responseCode = "403", description = "Access denied - admin role required",
                    content = @Content)
    })
    @PostMapping
    public ResponseEntity<PersonAdminResponse> createUser(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "User creation request",
                    required = true,
                    content = @Content(schema = @Schema(implementation = PersonCreateRequest.class))
            )
            @Valid @RequestBody PersonCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(personService.create(request));
    }

    @Operation(summary = "Update user",
            description = "Updates user password and/or role. At least one field must be provided. " +
                    "Password change increments passwordVersion, invalidating existing JWT tokens.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "User updated successfully",
                    content = @Content(schema = @Schema(implementation = PersonAdminResponse.class))),
            @ApiResponse(responseCode = "400", description = "No fields provided or invalid role",
                    content = @Content),
            @ApiResponse(responseCode = "403", description = "Access denied - admin role required",
                    content = @Content),
            @ApiResponse(responseCode = "404", description = "User not found",
                    content = @Content)
    })
    @PutMapping("/{id}")
    public ResponseEntity<PersonAdminResponse> updateUser(
            @Parameter(description = "User UUID", required = true)
            @PathVariable UUID id,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "User update request (password and/or role)",
                    required = true,
                    content = @Content(schema = @Schema(implementation = PersonUpdateRequest.class))
            )
            @Valid @RequestBody PersonUpdateRequest request) {
        return ResponseEntity.ok(personService.update(id, request));
    }

    @Operation(summary = "Delete user",
            description = "Deletes user. Fails if user has existing cards.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "User deleted successfully",
                    content = @Content),
            @ApiResponse(responseCode = "403", description = "Access denied - admin role required",
                    content = @Content),
            @ApiResponse(responseCode = "404", description = "User not found",
                    content = @Content),
            @ApiResponse(responseCode = "409", description = "User has existing cards",
                    content = @Content)
    })
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteUser(
            @Parameter(description = "User UUID", required = true)
            @PathVariable UUID id) {
        personService.delete(id);
    }
}