package com.example.bankcards.service;

import com.example.bankcards.dto.transfer.TransferHistoryResponse;
import com.example.bankcards.entity.TransferHistory;
import com.example.bankcards.repository.TransferHistoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@RequiredArgsConstructor
@Service
public class TransferHistoryService {
    private final TransferHistoryRepository transferHistoryRepository;

    @Transactional(readOnly = true)
    public Page<TransferHistoryResponse> getTransfers(UUID personId, Pageable pageable) {
        Page<TransferHistory> history;
        if (personId != null) {
            history = transferHistoryRepository.findByPerson_Id(personId, pageable);
        } else {
            history = transferHistoryRepository.findAll(pageable);
        }
        return history.map(this::toResponse);
    }

    private TransferHistoryResponse toResponse(TransferHistory entity) {
        return new TransferHistoryResponse(
                entity.getId(),
                entity.getPerson().getId(),
                entity.getFromCard().getId(),
                entity.getToCard().getId(),
                entity.getAmount(),
                entity.getTransactionDate()
        );
    }
}
