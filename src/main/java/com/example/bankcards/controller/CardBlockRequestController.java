package com.example.bankcards.controller;

import com.example.bankcards.dto.cardblockrequest.CardBlockRequestCreate;
import com.example.bankcards.dto.cardblockrequest.CardBlockRequestDto;
import com.example.bankcards.service.CardBlockRequestService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/block-requests")
@RequiredArgsConstructor
public class CardBlockRequestController {
    private final CardBlockRequestService cardBlockRequestService;

    @PostMapping
    public ResponseEntity<CardBlockRequestDto> createBlockRequest(@Valid @RequestBody CardBlockRequestCreate request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(cardBlockRequestService.createRequest(request));
    }

    @GetMapping("/pending")
    public ResponseEntity<Page<CardBlockRequestDto>> getPendingRequests(Pageable pageable) {
        return ResponseEntity.ok(cardBlockRequestService.getPendingRequests(pageable));
    }

    @PatchMapping("/{id}/approve")
    public ResponseEntity<CardBlockRequestDto> approveRequest(@PathVariable Long id) {
        return ResponseEntity.ok(cardBlockRequestService.approveRequest(id));
    }

    @PatchMapping("/{id}/reject")
    public ResponseEntity<CardBlockRequestDto> rejectRequest(@PathVariable Long id) {
        return ResponseEntity.ok(cardBlockRequestService.rejectRequest(id));
    }
}
