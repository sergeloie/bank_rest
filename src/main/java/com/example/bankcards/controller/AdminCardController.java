package com.example.bankcards.controller;

import com.example.bankcards.dto.card.CardAdminResponse;
import com.example.bankcards.dto.card.CardCreateRequest;
import com.example.bankcards.service.CardService;
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
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/admin/cards")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
@Tag(name = "Admin Cards", description = "Admin endpoints for managing all cards")
@SecurityRequirement(name = "bearerAuth")
public class AdminCardController {
    private final CardService cardService;

    @Operation(summary = "Get all cards with pagination and filtering",
            description = "Returns paginated list of all cards. Optional filters: personId (owner), status (ACTIVE, BLOCKED, EXPIRED)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Cards retrieved successfully",
                    content = @Content(schema = @Schema(implementation = Page.class))),
            @ApiResponse(responseCode = "403", description = "Access denied - admin role required",
                    content = @Content)
    })
    @GetMapping
    public ResponseEntity<Page<CardAdminResponse>> getAllCards(
            @Parameter(description = "Filter by card owner person ID")
            @RequestParam(required = false) UUID personId,
            @Parameter(description = "Filter by card status (ACTIVE, BLOCKED, EXPIRED)")
            @RequestParam(required = false) String status,
            Pageable pageable) {
        return ResponseEntity.ok(cardService.getAllCardsAdmin(personId, status, pageable));
    }

    @Operation(summary = "Get card by ID (admin view)",
            description = "Returns full card details including owner info, masked number, and audit fields")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Card found",
                    content = @Content(schema = @Schema(implementation = CardAdminResponse.class))),
            @ApiResponse(responseCode = "403", description = "Access denied - admin role required",
                    content = @Content),
            @ApiResponse(responseCode = "404", description = "Card not found",
                    content = @Content)
    })
    @GetMapping("/{id}")
    public ResponseEntity<CardAdminResponse> getCardById(
            @Parameter(description = "Card UUID", required = true)
            @PathVariable UUID id) {
        return ResponseEntity.ok(cardService.getCardByIdAdmin(id));
    }

    @Operation(summary = "Create new card for a user",
            description = "Creates a new card with generated number, encrypted storage, and initial balance. " +
                    "Card owner must be a USER (not ADMIN). Number is encrypted and masked in responses.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Card created successfully",
                    content = @Content(schema = @Schema(implementation = CardAdminResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid request (person is admin, invalid expiration date, etc.)",
                    content = @Content),
            @ApiResponse(responseCode = "403", description = "Access denied - admin role required",
                    content = @Content),
            @ApiResponse(responseCode = "404", description = "Person not found",
                    content = @Content)
    })
    @PostMapping
    public ResponseEntity<CardAdminResponse> createCard(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "Card creation request",
                    required = true,
                    content = @Content(schema = @Schema(implementation = CardCreateRequest.class))
            )
            @Valid @RequestBody CardCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(cardService.createCard(request));
    }

    @Operation(summary = "Block an active card",
            description = "Changes card status from ACTIVE to BLOCKED. Only active cards can be blocked.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Card blocked successfully",
                    content = @Content(schema = @Schema(implementation = CardAdminResponse.class))),
            @ApiResponse(responseCode = "400", description = "Card is not active",
                    content = @Content),
            @ApiResponse(responseCode = "403", description = "Access denied - admin role required",
                    content = @Content),
            @ApiResponse(responseCode = "404", description = "Card not found",
                    content = @Content)
    })
    @PatchMapping("/{id}/block")
    public ResponseEntity<CardAdminResponse> blockCard(
            @Parameter(description = "Card UUID", required = true)
            @PathVariable UUID id) {
        return ResponseEntity.ok(cardService.blockCard(id));
    }

    @Operation(summary = "Activate a blocked card",
            description = "Changes card status from BLOCKED to ACTIVE. Only blocked cards can be activated.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Card activated successfully",
                    content = @Content(schema = @Schema(implementation = CardAdminResponse.class))),
            @ApiResponse(responseCode = "400", description = "Card is not blocked",
                    content = @Content),
            @ApiResponse(responseCode = "403", description = "Access denied - admin role required",
                    content = @Content),
            @ApiResponse(responseCode = "404", description = "Card not found",
                    content = @Content)
    })
    @PatchMapping("/{id}/activate")
    public ResponseEntity<CardAdminResponse> activateCard(
            @Parameter(description = "Card UUID", required = true)
            @PathVariable UUID id) {
        return ResponseEntity.ok(cardService.activateCard(id));
    }

    @Operation(summary = "Delete a card",
            description = "Permanently deletes a card. Cannot delete card if it has transfer history.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Card deleted successfully",
                    content = @Content),
            @ApiResponse(responseCode = "403", description = "Access denied - admin role required",
                    content = @Content),
            @ApiResponse(responseCode = "404", description = "Card not found",
                    content = @Content)
    })
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteCard(
            @Parameter(description = "Card UUID", required = true)
            @PathVariable UUID id) {
        cardService.deleteCard(id);
    }
}