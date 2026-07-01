package com.example.bankcards.controller;

import com.example.bankcards.dto.card.CardCreateRequest;
import com.example.bankcards.dto.card.CardResponse;
import com.example.bankcards.dto.card.CardTransferRequest;
import com.example.bankcards.dto.card.CardTransferResponse;
import com.example.bankcards.service.CardService;
import com.example.bankcards.service.TransferService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/cards")
@RequiredArgsConstructor
public class CardController {
    private final CardService cardService;
    private final TransferService transferService;

    @PreAuthorize("hasRole('ADMIN') or @cardSecurity.isOwnerByPersonId(#personId, authentication)")
    @GetMapping("/person/{personId}")
    public ResponseEntity<Page<CardResponse>> getCardsByPerson(@PathVariable Long personId, Pageable pageable) {
        return ResponseEntity.ok(cardService.getCardsByPerson(personId, pageable));
    }

    @PreAuthorize("hasRole('ADMIN') or @cardSecurity.isOwner(#id, authentication)")
    @GetMapping("/{id}")
    public ResponseEntity<CardResponse> getCardById(@PathVariable Long id) {
        return ResponseEntity.ok(cardService.getCardById(id));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping
    public ResponseEntity<CardResponse> createCard(@Valid @RequestBody CardCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(cardService.createCard(request));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PatchMapping("/{id}/block")
    public ResponseEntity<CardResponse> blockCard(@PathVariable Long id) {
        return ResponseEntity.ok(cardService.blockCard(id));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PatchMapping("/{id}/activate")
    public ResponseEntity<CardResponse> activateCard(@PathVariable Long id) {
        return ResponseEntity.ok(cardService.activateCard(id));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteCard(@PathVariable Long id) {
        cardService.deleteCard(id);
    }

    @PreAuthorize("hasRole('ADMIN') or @cardSecurity.isOwner(#request.fromCardId, authentication)")
    @PostMapping("/transfer")
    public ResponseEntity<CardTransferResponse> transfer(@Valid @RequestBody CardTransferRequest request) {
        return ResponseEntity.ok(transferService.transfer(request));
    }
}
