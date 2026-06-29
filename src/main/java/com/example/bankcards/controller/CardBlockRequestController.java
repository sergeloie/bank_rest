package com.example.bankcards.controller;

import com.example.bankcards.dto.cardblockrequest.CardBlockRequestRequest;
import com.example.bankcards.dto.cardblockrequest.CardBlockRequestResponse;
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
    public ResponseEntity<CardBlockRequestResponse> createBlockRequest(@Valid @RequestBody CardBlockRequestRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(cardBlockRequestService.createRequest(request));
    }

    @GetMapping("/pending")
    public ResponseEntity<Page<CardBlockRequestResponse>> getPendingRequests(Pageable pageable) {
        return ResponseEntity.ok(cardBlockRequestService.getPendingRequests(pageable));
    }

    @PatchMapping("/{id}/approve")
    public ResponseEntity<CardBlockRequestResponse> approveRequest(@PathVariable Long id) {
        return ResponseEntity.ok(cardBlockRequestService.approveRequest(id));
    }

    @PatchMapping("/{id}/reject")
    public ResponseEntity<CardBlockRequestResponse> rejectRequest(@PathVariable Long id) {
        return ResponseEntity.ok(cardBlockRequestService.rejectRequest(id));
    }
}
