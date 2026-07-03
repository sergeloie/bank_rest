package com.example.bankcards.controller;

import com.example.bankcards.dto.cardblockrequest.CardBlockRequestAdminResponse;
import com.example.bankcards.service.CardBlockRequestService;
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
public class AdminCardBlockRequestController {
    private final CardBlockRequestService cardBlockRequestService;

    @GetMapping("/pending")
    public ResponseEntity<Page<CardBlockRequestAdminResponse>> getPendingRequests(Pageable pageable) {
        return ResponseEntity.ok(cardBlockRequestService.getPendingRequests(pageable));
    }

    @PatchMapping("/{id}/approve")
    public ResponseEntity<CardBlockRequestAdminResponse> approveRequest(@PathVariable Long id) {
        return ResponseEntity.ok(cardBlockRequestService.approveRequest(id));
    }

    @PatchMapping("/{id}/reject")
    public ResponseEntity<CardBlockRequestAdminResponse> rejectRequest(@PathVariable Long id) {
        return ResponseEntity.ok(cardBlockRequestService.rejectRequest(id));
    }
}
