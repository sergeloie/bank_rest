package com.example.bankcards.controller;

import com.example.bankcards.dto.cardblockrequest.CardBlockRequestAdminResponse;
import com.example.bankcards.service.CardBlockRequestService;
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
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/block-requests")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
@Tag(name = "Admin Block Requests", description = "Admin endpoints for managing card block requests")
@SecurityRequirement(name = "bearerAuth")
public class AdminCardBlockRequestController {
    private final CardBlockRequestService cardBlockRequestService;

    @Operation(summary = "Get all pending block requests",
            description = "Returns paginated list of card block requests with PENDING status. " +
                    "Includes card details (masked number), owner info, and request metadata.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Pending requests retrieved successfully",
                    content = @Content(schema = @Schema(implementation = Page.class))),
            @ApiResponse(responseCode = "403", description = "Access denied - admin role required",
                    content = @Content)
    })
    @GetMapping("/pending")
    public ResponseEntity<Page<CardBlockRequestAdminResponse>> getPendingRequests(Pageable pageable) {
        return ResponseEntity.ok(cardBlockRequestService.getPendingRequests(pageable));
    }

    @Operation(summary = "Approve a block request",
            description = "Changes request status to APPROVED and sets associated card status to BLOCKED. " +
                    "Only PENDING requests can be approved.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Request approved successfully",
                    content = @Content(schema = @Schema(implementation = CardBlockRequestAdminResponse.class))),
            @ApiResponse(responseCode = "400", description = "Request is not in PENDING status",
                    content = @Content),
            @ApiResponse(responseCode = "403", description = "Access denied - admin role required",
                    content = @Content),
            @ApiResponse(responseCode = "404", description = "Request not found",
                    content = @Content)
    })
    @PatchMapping("/{id}/approve")
    public ResponseEntity<CardBlockRequestAdminResponse> approveRequest(
            @Parameter(description = "Block request ID", required = true)
            @PathVariable Long id) {
        return ResponseEntity.ok(cardBlockRequestService.approveRequest(id));
    }

    @Operation(summary = "Reject a block request",
            description = "Changes request status to REJECTED. Card status remains unchanged. " +
                    "Only PENDING requests can be rejected.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Request rejected successfully",
                    content = @Content(schema = @Schema(implementation = CardBlockRequestAdminResponse.class))),
            @ApiResponse(responseCode = "400", description = "Request is not in PENDING status",
                    content = @Content),
            @ApiResponse(responseCode = "403", description = "Access denied - admin role required",
                    content = @Content),
            @ApiResponse(responseCode = "404", description = "Request not found",
                    content = @Content)
    })
    @PatchMapping("/{id}/reject")
    public ResponseEntity<CardBlockRequestAdminResponse> rejectRequest(
            @Parameter(description = "Block request ID", required = true)
            @PathVariable Long id) {
        return ResponseEntity.ok(cardBlockRequestService.rejectRequest(id));
    }
}