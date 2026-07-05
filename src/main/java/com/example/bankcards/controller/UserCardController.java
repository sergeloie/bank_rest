package com.example.bankcards.controller;

import com.example.bankcards.dto.card.CardResponse;
import com.example.bankcards.dto.cardblockrequest.CardBlockRequestRequest;
import com.example.bankcards.dto.cardblockrequest.CardBlockRequestResponse;
import com.example.bankcards.entity.Person;
import com.example.bankcards.service.CardBlockRequestService;
import com.example.bankcards.service.CardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/cards")
@PreAuthorize("hasRole('USER')")
@RequiredArgsConstructor
@Tag(name = "User Cards", description = "Endpoints for users to manage their own cards")
@SecurityRequirement(name = "bearerAuth")
public class UserCardController {
    private final CardService cardService;
    private final CardBlockRequestService cardBlockRequestService;

    @Operation(summary = "Get user's cards with pagination and filtering",
            description = "Returns paginated list of cards owned by the authenticated user. " +
                    "Optional status filter: ACTIVE, BLOCKED, EXPIRED")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Cards retrieved successfully",
                    content = @Content(schema = @Schema(implementation = Page.class))),
            @ApiResponse(responseCode = "403", description = "Access denied - not owner of cards",
                    content = @Content)
    })
    @PreAuthorize("@cardSecurity.isOwnerByPersonId(#personId, authentication)")
    @GetMapping("/person/{personId}")
    public ResponseEntity<Page<CardResponse>> getMyCards(
            @Parameter(description = "UUID of the person (must match authenticated user)", required = true)
            @PathVariable UUID personId,
            @Parameter(description = "Optional card status filter (ACTIVE, BLOCKED, EXPIRED)")
            @RequestParam(required = false) String status,
            Pageable pageable) {
        return ResponseEntity.ok(cardService.getCardsByPersonUser(personId, status, pageable));
    }

    @Operation(summary = "Get single card by ID",
            description = "Returns card details for the authenticated user. " +
                    "Card number is masked (**** **** **** 1234)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Card found",
                    content = @Content(schema = @Schema(implementation = CardResponse.class))),
            @ApiResponse(responseCode = "403", description = "Access denied - not card owner",
                    content = @Content),
            @ApiResponse(responseCode = "404", description = "Card not found",
                    content = @Content)
    })
    @PreAuthorize("@cardSecurity.isOwner(#id, authentication)")
    @GetMapping("/{id}")
    public ResponseEntity<CardResponse> getMyCardById(
            @Parameter(description = "Card UUID", required = true)
            @PathVariable UUID id) {
        return ResponseEntity.ok(cardService.getCardByIdUser(id));
    }

    @Operation(summary = "Request card block",
            description = "Creates a card block request for the authenticated user's card. " +
                    "Admin must approve/reject the request.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Block request created",
                    content = @Content(schema = @Schema(implementation = CardBlockRequestResponse.class))),
            @ApiResponse(responseCode = "400", description = "Card is not active or already blocked",
                    content = @Content),
            @ApiResponse(responseCode = "403", description = "Access denied - not card owner",
                    content = @Content),
            @ApiResponse(responseCode = "404", description = "Card not found",
                    content = @Content)
    })
    @PreAuthorize("@cardSecurity.isOwner(#id, authentication)")
    @PutMapping("/{id}/block-request")
    public ResponseEntity<CardBlockRequestResponse> requestBlock(
            @Parameter(description = "Card UUID to block", required = true)
            @PathVariable UUID id,
            Authentication authentication) {
        Person person = (Person) authentication.getPrincipal();
        CardBlockRequestRequest request = new CardBlockRequestRequest(id, person.getId());
        return ResponseEntity.ok(cardBlockRequestService.createRequest(request));
    }
}