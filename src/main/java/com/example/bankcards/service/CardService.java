package com.example.bankcards.service;

import com.example.bankcards.dto.card.CardAdminResponse;
import com.example.bankcards.dto.card.CardCreateRequest;
import com.example.bankcards.dto.card.CardResponse;
import com.example.bankcards.entity.Card;
import com.example.bankcards.entity.CardStatus;
import com.example.bankcards.entity.Person;
import org.springframework.util.StringUtils;
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
    public Page<CardAdminResponse> getCardsByPersonAdmin(Long personId, String status, Pageable pageable) {
        return getCardsByPersonInternal(personId, status, pageable)
                .map(this::toAdminResponse);
    }

    @Transactional(readOnly = true)
    public Page<CardResponse> getCardsByPersonUser(Long personId, String status, Pageable pageable) {
        return getCardsByPersonInternal(personId, status, pageable)
                .map(this::toUserResponse);
    }

    @Transactional(readOnly = true)
    public CardAdminResponse getCardByIdAdmin(Long id) {
        Card card = cardRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(String.format(CARD_NOT_FOUND, id)));
        return toAdminResponse(card);
    }

    @Transactional(readOnly = true)
    public CardResponse getCardByIdUser(Long id) {
        Card card = cardRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(String.format(CARD_NOT_FOUND, id)));
        return toUserResponse(card);
    }

    @Transactional
    public CardAdminResponse createCard(CardCreateRequest request) {
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
        return toAdminResponse(saved);
    }

    @Transactional
    public CardAdminResponse blockCard(Long id) {
        Card card = cardRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(String.format(CARD_NOT_FOUND, id)));

        if (card.getCardStatus() != CardStatus.ACTIVE) {
            throw new InvalidCardOperationException("Only active cards can be blocked");
        }

        card.setCardStatus(CardStatus.BLOCKED);
        return toAdminResponse(card);
    }

    @Transactional
    public CardAdminResponse activateCard(Long id) {
        Card card = cardRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(String.format(CARD_NOT_FOUND, id)));

        if (card.getCardStatus() != CardStatus.BLOCKED) {
            throw new InvalidCardOperationException("Only blocked cards can be activated");
        }

        card.setCardStatus(CardStatus.ACTIVE);
        return toAdminResponse(card);
    }

    @Transactional
    public void deleteCard(Long id) {
        Card card = cardRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(String.format(CARD_NOT_FOUND, id)));
        cardRepository.delete(card);
    }

    private Page<Card> getCardsByPersonInternal(Long personId, String status, Pageable pageable) {
        if (StringUtils.hasText(status)) {
            CardStatus cardStatus = CardStatus.valueOf(status.toUpperCase());
            return cardRepository.findByPerson_IdAndCardStatus(personId, cardStatus, pageable);
        }
        return cardRepository.findByPerson_Id(personId, pageable);
    }

    private String computeMaskedNumber(Card card) {
        String plainNumber = cardEncryptionUtil.decrypt(card.getEncryptedNumber());
        return cardMaskUtil.mask(plainNumber);
    }

    private CardAdminResponse toAdminResponse(Card card) {
        String masked = computeMaskedNumber(card);
        return new CardAdminResponse(
                card.getId(),
                card.getPerson().getId(),
                card.getPerson().getName(),
                masked,
                card.getExpirationDate(),
                card.getCardStatus(),
                card.getBalance(),
                card.getCreatedDate(),
                card.getLastModifiedDate(),
                card.getCreatedBy(),
                card.getModifiedBy()
        );
    }

    private CardResponse toUserResponse(Card card) {
        String masked = computeMaskedNumber(card);
        return new CardResponse(
                masked,
                card.getExpirationDate(),
                card.getCardStatus(),
                card.getBalance()
        );
    }
}
