package com.example.bankcards.controller;

import com.example.bankcards.dto.card.CardTransferRequest;
import com.example.bankcards.dto.card.CardTransferResponse;
import com.example.bankcards.entity.Person;
import com.example.bankcards.service.TransferService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/transfers")
@PreAuthorize("hasRole('USER')")
@RequiredArgsConstructor
@Tag(name = "User Transfers", description = "Endpoints for users to transfer funds between their own cards")
@SecurityRequirement(name = "bearerAuth")
public class UserTransferController {
    private final TransferService transferService;

    @Operation(summary = "Transfer funds between own cards",
            description = "Transfers money from one card to another. Both cards must belong to the authenticated user. " +
                    "Source and target cards must be ACTIVE. Sufficient balance required on source card.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Transfer completed successfully",
                    content = @Content(schema = @Schema(implementation = CardTransferResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid request (same card, insufficient funds, inactive cards, etc.)",
                    content = @Content),
            @ApiResponse(responseCode = "403", description = "Access denied - cards don't belong to user",
                    content = @Content),
            @ApiResponse(responseCode = "404", description = "Source or target card not found",
                    content = @Content)
    })
    @PostMapping
    public ResponseEntity<CardTransferResponse> transfer(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "Transfer details",
                    required = true,
                    content = @Content(schema = @Schema(implementation = CardTransferRequest.class))
            )
            @Valid @RequestBody CardTransferRequest request,
            Authentication authentication) {
        Person person = (Person) authentication.getPrincipal();
        return ResponseEntity.ok(transferService.transfer(request, person.getId()));
    }
}