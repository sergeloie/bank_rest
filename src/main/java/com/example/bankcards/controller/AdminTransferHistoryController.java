package com.example.bankcards.controller;

import com.example.bankcards.dto.transfer.TransferHistoryResponse;
import com.example.bankcards.service.TransferHistoryService;
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
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/admin/transfers")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
@Tag(name = "Admin Transfers", description = "Admin endpoints for viewing transfer history")
@SecurityRequirement(name = "bearerAuth")
public class AdminTransferHistoryController {
    private final TransferHistoryService transferHistoryService;

    @Operation(summary = "Get all transfers with pagination and filtering",
            description = "Returns paginated list of all transfers. Optional filter by personId to see transfers for a specific user.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Transfers retrieved successfully",
                    content = @Content(schema = @Schema(implementation = Page.class))),
            @ApiResponse(responseCode = "403", description = "Access denied - admin role required",
                    content = @Content)
    })
    @GetMapping
    public ResponseEntity<Page<TransferHistoryResponse>> getAllTransfers(
            @Parameter(description = "Filter by person ID (optional)")
            @RequestParam(required = false) UUID personId,
            Pageable pageable) {
        return ResponseEntity.ok(transferHistoryService.getTransfers(personId, pageable));
    }
}