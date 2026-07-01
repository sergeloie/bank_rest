package com.example.bankcards.service;

import com.example.bankcards.dto.card.CardTransferRequest;
import com.example.bankcards.dto.card.CardTransferResponse;
import com.example.bankcards.entity.Card;
import com.example.bankcards.entity.CardStatus;
import com.example.bankcards.exception.InvalidCardOperationException;
import com.example.bankcards.exception.ResourceNotFoundException;
import com.example.bankcards.repository.CardRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@RequiredArgsConstructor
@Service
public class TransferService {
    private final CardRepository cardRepository;

    @Transactional
    public CardTransferResponse transfer(CardTransferRequest request) {
        if (request.fromCardId().equals(request.toCardId())) {
            throw new InvalidCardOperationException("Cannot transfer to the same card");
        }

        // Deterministic lock order: lock lower ID first to prevent deadlock
        Long firstId = Math.min(request.fromCardId(), request.toCardId());
        Long secondId = Math.max(request.fromCardId(), request.toCardId());

        Card firstCard = cardRepository.findByIdForUpdate(firstId)
                .orElseThrow(() -> new ResourceNotFoundException("Card not found with id: " + firstId));
        Card secondCard = cardRepository.findByIdForUpdate(secondId)
                .orElseThrow(() -> new ResourceNotFoundException("Card not found with id: " + secondId));

        Card fromCard = firstCard.getId().equals(request.fromCardId()) ? firstCard : secondCard;
        Card toCard = firstCard.getId().equals(request.toCardId()) ? firstCard : secondCard;

        if (!fromCard.getPerson().getId().equals(toCard.getPerson().getId())) {
            throw new InvalidCardOperationException("Can only transfer between own cards");
        }

        if (!fromCard.getPerson().getId().equals(request.personId())) {
            throw new InvalidCardOperationException("Cards do not belong to this person");
        }

        if (fromCard.getCardStatus() != CardStatus.ACTIVE) {
            throw new InvalidCardOperationException("Source card must be active");
        }

        if (toCard.getCardStatus() != CardStatus.ACTIVE) {
            throw new InvalidCardOperationException("Target card must be active");
        }

        if (fromCard.getBalance().compareTo(request.amount()) < 0) {
            throw new InvalidCardOperationException("Insufficient funds on source card");
        }

        fromCard.setBalance(fromCard.getBalance().subtract(request.amount()));
        toCard.setBalance(toCard.getBalance().add(request.amount()));

        return new CardTransferResponse(
                fromCard.getId(),
                toCard.getId(),
                request.amount(),
                fromCard.getBalance(),
                toCard.getBalance()
        );
    }
}
