package com.example.bankcards.controller;

import com.example.bankcards.dto.card.CardAdminResponse;
import com.example.bankcards.dto.card.CardCreateRequest;
import com.example.bankcards.service.CardService;
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
public class AdminCardController {
    private final CardService cardService;

    @GetMapping
    public ResponseEntity<Page<CardAdminResponse>> getAllCards(
            @RequestParam(required = false) UUID personId,
            @RequestParam(required = false) String status,
            Pageable pageable) {
        return ResponseEntity.ok(cardService.getAllCardsAdmin(personId, status, pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<CardAdminResponse> getCardById(@PathVariable UUID id) {
        return ResponseEntity.ok(cardService.getCardByIdAdmin(id));
    }

    @PostMapping
    public ResponseEntity<CardAdminResponse> createCard(@Valid @RequestBody CardCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(cardService.createCard(request));
    }

    @PutMapping("/{id}/block")
    public ResponseEntity<CardAdminResponse> blockCard(@PathVariable UUID id) {
        return ResponseEntity.ok(cardService.blockCard(id));
    }

    @PutMapping("/{id}/activate")
    public ResponseEntity<CardAdminResponse> activateCard(@PathVariable UUID id) {
        return ResponseEntity.ok(cardService.activateCard(id));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteCard(@PathVariable UUID id) {
        cardService.deleteCard(id);
    }
}
