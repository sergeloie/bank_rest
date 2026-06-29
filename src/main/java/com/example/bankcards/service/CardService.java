package com.example.bankcards.service;

import com.example.bankcards.dto.card.CardCreateRequest;
import com.example.bankcards.dto.card.CardResponse;
import com.example.bankcards.entity.Card;
import com.example.bankcards.entity.CardStatus;
import com.example.bankcards.entity.Person;
import com.example.bankcards.exception.InvalidCardOperationException;
import com.example.bankcards.exception.ResourceNotFoundException;
import com.example.bankcards.mapper.CardMapper;
import com.example.bankcards.repository.CardRepository;
import com.example.bankcards.repository.PersonRepository;
import com.example.bankcards.util.CardEncryptionUtil;
import com.example.bankcards.util.CardNumberGenerator;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.ZoneId;

@RequiredArgsConstructor
@Service
public class CardService {
    private final CardRepository cardRepository;
    private final PersonRepository personRepository;
    private final CardMapper cardMapper;
    private final CardNumberGenerator cardNumberGenerator;
    private final CardEncryptionUtil cardEncryptionUtil;
    private final static String personNotFound = "Person not found with id: %d";
    private final static String cardNotFound = "Card not found with id: %d";

    public Page<CardResponse> getCardsByPerson(Long personId, Pageable pageable) {
        if (!personRepository.existsById(personId)) {
            throw new ResourceNotFoundException(String.format(personNotFound, personId));
        }
        return cardRepository.findByPerson_Id(personId, pageable).map(cardMapper::toCardResponse);
    }

    public CardResponse getCardById(Long id) {
        Card card = cardRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(String.format(cardNotFound, id)));
        return cardMapper.toCardResponse(card);
    }

    public CardResponse createCard(CardCreateRequest request) {
        Person person = personRepository.findById(request.personId())
                .orElseThrow(() -> new ResourceNotFoundException(String.format(personNotFound, request.personId())));

        if (request.expirationDate().isBefore(LocalDate.now(ZoneId.systemDefault()))) {
            throw new InvalidCardOperationException("Expiration date must not be in the past");
        }

        String plainNumber = cardNumberGenerator.generate();
        String encryptedNumber = cardEncryptionUtil.encrypt(plainNumber);

        Card card = new Card();
        card.setPerson(person);
        card.setExpirationDate(request.expirationDate());
        card.setEncryptedNumber(encryptedNumber);
        card.setCardStatus(CardStatus.ACTIVE);
        card.setBalance(request.balance() != null ? request.balance() : java.math.BigDecimal.ZERO);

        return cardMapper.toCardResponse(cardRepository.save(card));
    }

    public CardResponse blockCard(Long id) {
        Card card = cardRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(String.format(cardNotFound, id)));

        if (card.getCardStatus() != CardStatus.ACTIVE) {
            throw new InvalidCardOperationException("Only active cards can be blocked");
        }

        card.setCardStatus(CardStatus.BLOCKED);
        return cardMapper.toCardResponse(cardRepository.save(card));
    }

    public CardResponse activateCard(Long id) {
        Card card = cardRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(String.format(cardNotFound, id)));

        if (card.getCardStatus() != CardStatus.BLOCKED) {
            throw new InvalidCardOperationException("Only blocked cards can be activated");
        }

        card.setCardStatus(CardStatus.ACTIVE);
        return cardMapper.toCardResponse(cardRepository.save(card));
    }

    public void deleteCard(Long id) {
        Card card = cardRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(String.format(cardNotFound, id)));
        cardRepository.delete(card);
    }
}
