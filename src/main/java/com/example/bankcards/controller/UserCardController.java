package com.example.bankcards.controller;

import com.example.bankcards.dto.card.CardResponse;
import com.example.bankcards.dto.cardblockrequest.CardBlockRequestRequest;
import com.example.bankcards.dto.cardblockrequest.CardBlockRequestResponse;
import com.example.bankcards.entity.Person;
import com.example.bankcards.service.CardBlockRequestService;
import com.example.bankcards.service.CardService;
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
public class UserCardController {
    private final CardService cardService;
    private final CardBlockRequestService cardBlockRequestService;

    @PreAuthorize("@cardSecurity.isOwnerByPersonId(#personId, authentication)")
    @GetMapping("/person/{personId}")
    public ResponseEntity<Page<CardResponse>> getMyCards(
            @PathVariable UUID personId,
            @RequestParam(required = false) String status,
            Pageable pageable) {
        return ResponseEntity.ok(cardService.getCardsByPersonUser(personId, status, pageable));
    }

    @PreAuthorize("@cardSecurity.isOwner(#id, authentication)")
    @GetMapping("/{id}")
    public ResponseEntity<CardResponse> getMyCardById(@PathVariable UUID id) {
        return ResponseEntity.ok(cardService.getCardByIdUser(id));
    }

    @PreAuthorize("@cardSecurity.isOwner(#id, authentication)")
    @PutMapping("/{id}/block-request")
    public ResponseEntity<CardBlockRequestResponse> requestBlock(
            @PathVariable UUID id,
            Authentication authentication) {
        Person person = (Person) authentication.getPrincipal();
        CardBlockRequestRequest request = new CardBlockRequestRequest(id, person.getId());
        return ResponseEntity.ok(cardBlockRequestService.createRequest(request));
    }
}
