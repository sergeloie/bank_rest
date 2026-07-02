package com.example.bankcards.controller;

import com.example.bankcards.dto.transfer.TransferHistoryResponse;
import com.example.bankcards.service.TransferHistoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/admin/transfers")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AdminTransferHistoryController {
    private final TransferHistoryService transferHistoryService;

    @GetMapping
    public ResponseEntity<Page<TransferHistoryResponse>> getAllTransfers(
            @RequestParam(required = false) UUID personId,
            Pageable pageable) {
        return ResponseEntity.ok(transferHistoryService.getTransfers(personId, pageable));
    }
}
