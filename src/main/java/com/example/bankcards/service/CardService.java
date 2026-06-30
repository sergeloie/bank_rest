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
import com.example.bankcards.util.CardMaskUtil;
import com.example.bankcards.util.CardNumberGenerator;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
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
    private final CardMaskUtil cardMaskUtil;
    private static final String PERSON_NOT_FOUND = "Person not found with id: %d";
    private static final String CARD_NOT_FOUND = "Card not found with id: %d";

    @Transactional(readOnly = true)
    public Page<CardResponse> getCardsByPerson(Long personId, Pageable pageable) {
        return cardRepository.findByPerson_Id(personId, pageable)
                .map(card -> enrichWithMaskedNumber(cardMapper.toCardResponse(card), card));
    }

    @Transactional(readOnly = true)
    public CardResponse getCardById(Long id) {
        Card card = cardRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(String.format(CARD_NOT_FOUND, id)));
        return enrichWithMaskedNumber(cardMapper.toCardResponse(card), card);
    }

    @Transactional
    public CardResponse createCard(CardCreateRequest request) {
        Person person = personRepository.findById(request.personId())
                .orElseThrow(() -> new ResourceNotFoundException(String.format(PERSON_NOT_FOUND, request.personId())));

        if (request.expirationDate().isBefore(LocalDate.now(ZoneId.systemDefault()))) {
            throw new InvalidCardOperationException("Expiration date must not be in the past");
        }

        String plainNumber = cardNumberGenerator.generate();
        String encryptedNumber = cardEncryptionUtil.encrypt(plainNumber);
        String cardHash = cardEncryptionUtil.hash(plainNumber);

        Card card = new Card();
        card.setPerson(person);
        card.setExpirationDate(request.expirationDate());
        card.setEncryptedNumber(encryptedNumber);
        card.setCardHash(cardHash);
        card.setCardStatus(CardStatus.ACTIVE);
        card.setBalance(request.balance() != null ? request.balance() : BigDecimal.ZERO);

        Card saved = cardRepository.save(card);
        return enrichWithMaskedNumber(cardMapper.toCardResponse(saved), saved);
    }

    @Transactional
    public CardResponse blockCard(Long id) {
        Card card = cardRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(String.format(CARD_NOT_FOUND, id)));

        if (card.getCardStatus() != CardStatus.ACTIVE) {
            throw new InvalidCardOperationException("Only active cards can be blocked");
        }

        card.setCardStatus(CardStatus.BLOCKED);
        return enrichWithMaskedNumber(cardMapper.toCardResponse(card), card);
    }

    @Transactional
    public CardResponse activateCard(Long id) {
        Card card = cardRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(String.format(CARD_NOT_FOUND, id)));

        if (card.getCardStatus() != CardStatus.BLOCKED) {
            throw new InvalidCardOperationException("Only blocked cards can be activated");
        }

        card.setCardStatus(CardStatus.ACTIVE);
        return enrichWithMaskedNumber(cardMapper.toCardResponse(card), card);
    }

    @Transactional
    public void deleteCard(Long id) {
        Card card = cardRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(String.format(CARD_NOT_FOUND, id)));
        cardRepository.delete(card);
    }

    private CardResponse enrichWithMaskedNumber(CardResponse response, Card card) {
        String plainNumber = cardEncryptionUtil.decrypt(card.getEncryptedNumber());
        String masked = cardMaskUtil.mask(plainNumber);
        return new CardResponse(
                response.id(),
                response.personId(),
                response.personName(),
                masked,
                response.expirationDate(),
                response.cardStatus(),
                response.balance()
        );
    }
}
