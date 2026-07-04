package com.example.bankcards.service;

import com.example.bankcards.dto.card.CardTransferRequest;
import com.example.bankcards.dto.card.CardTransferResponse;
import com.example.bankcards.entity.Card;
import com.example.bankcards.entity.CardStatus;
import com.example.bankcards.entity.TransferHistory;
import com.example.bankcards.exception.InvalidCardOperationException;
import com.example.bankcards.exception.ResourceNotFoundException;
import com.example.bankcards.repository.CardRepository;
import com.example.bankcards.repository.TransferHistoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Slf4j
@RequiredArgsConstructor
@Service
public class TransferService {
    private final CardRepository cardRepository;
    private final TransferHistoryRepository transferHistoryRepository;

    @Transactional
    public CardTransferResponse transfer(CardTransferRequest request) {
        log.info("Transfer initiated: fromCard={}, toCard={}, personId={}",
                request.fromCardId(), request.toCardId(), request.personId());

        if (request.fromCardId().equals(request.toCardId())) {
            log.warn("Transfer rejected: cannot transfer to the same card (cardId={})", request.fromCardId());
            throw new InvalidCardOperationException("Cannot transfer to the same card");
        }

        UUID firstId = request.fromCardId().compareTo(request.toCardId()) < 0 ? request.fromCardId() : request.toCardId();
        UUID secondId = request.fromCardId().compareTo(request.toCardId()) < 0 ? request.toCardId() : request.fromCardId();

        Card firstCard = cardRepository.findByIdForUpdate(firstId)
                .orElseThrow(() -> {
                    log.warn("Transfer rejected: source card not found (cardId={})", firstId);
                    return new ResourceNotFoundException("Card not found with id: " + firstId);
                });
        Card secondCard = cardRepository.findByIdForUpdate(secondId)
                .orElseThrow(() -> {
                    log.warn("Transfer rejected: target card not found (cardId={})", secondId);
                    return new ResourceNotFoundException("Card not found with id: " + secondId);
                });

        Card fromCard = firstCard.getId().equals(request.fromCardId()) ? firstCard : secondCard;
        Card toCard = firstCard.getId().equals(request.toCardId()) ? firstCard : secondCard;

        if (!fromCard.getPerson().getId().equals(toCard.getPerson().getId())) {
            log.warn("Transfer rejected: cards do not belong to the same person (fromPerson={}, toPerson={})",
                    fromCard.getPerson().getId(), toCard.getPerson().getId());
            throw new InvalidCardOperationException("Can only transfer between own cards");
        }

        if (!fromCard.getPerson().getId().equals(request.personId())) {
            log.warn("Transfer rejected: cards do not belong to person {}", request.personId());
            throw new InvalidCardOperationException("Cards do not belong to this person");
        }

        if (fromCard.getCardStatus() != CardStatus.ACTIVE) {
            log.warn("Transfer rejected: source card {} is not active (status={})", fromCard.getId(), fromCard.getCardStatus());
            throw new InvalidCardOperationException("Source card must be active");
        }

        if (toCard.getCardStatus() != CardStatus.ACTIVE) {
            log.warn("Transfer rejected: target card {} is not active (status={})", toCard.getId(), toCard.getCardStatus());
            throw new InvalidCardOperationException("Target card must be active");
        }

        if (fromCard.getBalance().compareTo(request.amount()) < 0) {
            log.warn("Transfer rejected: insufficient funds on card {}, requested={}, available={}",
                    fromCard.getId(), request.amount(), fromCard.getBalance());
            throw new InvalidCardOperationException("Insufficient funds on source card");
        }

        fromCard.setBalance(fromCard.getBalance().subtract(request.amount()));
        toCard.setBalance(toCard.getBalance().add(request.amount()));

        TransferHistory history = new TransferHistory();
        history.setPerson(fromCard.getPerson());
        history.setFromCard(fromCard);
        history.setToCard(toCard);
        history.setAmount(request.amount());
        transferHistoryRepository.save(history);

        log.info("Transfer completed: fromCard={}, toCard={}, amount={}",
                request.fromCardId(), request.toCardId(), request.amount());

        return new CardTransferResponse(
                fromCard.getId(),
                toCard.getId(),
                request.amount(),
                fromCard.getBalance(),
                toCard.getBalance()
        );
    }
}
