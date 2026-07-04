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
@PreAuthorize("hasRole('USER')")
@RequiredArgsConstructor
public class UserTransferController {
    private final TransferService transferService;

    @PostMapping
    public ResponseEntity<CardTransferResponse> transfer(
            @Valid @RequestBody CardTransferRequest request,
            Authentication authentication) {
        Person person = (Person) authentication.getPrincipal();
        return ResponseEntity.ok(transferService.transfer(request, person.getId()));
    }
}
