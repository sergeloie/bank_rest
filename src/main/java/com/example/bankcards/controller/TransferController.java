package com.example.bankcards.controller;

import com.example.bankcards.dto.card.CardTransferRequest;
import com.example.bankcards.dto.card.CardTransferResponse;
import com.example.bankcards.entity.Person;
import com.example.bankcards.service.TransferService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/transfers")
@RequiredArgsConstructor
public class TransferController {
    private final TransferService transferService;

    @PreAuthorize("hasRole('USER')")
    @PostMapping
    public ResponseEntity<CardTransferResponse> transfer(
            @Valid @RequestBody CardTransferRequest request,
            Authentication authentication) {
        Person person = (Person) authentication.getPrincipal();
        CardTransferRequest authRequest = new CardTransferRequest(
                request.fromCardId(), request.toCardId(), request.amount(), person.getId());
        return ResponseEntity.ok(transferService.transfer(authRequest));
    }
}
