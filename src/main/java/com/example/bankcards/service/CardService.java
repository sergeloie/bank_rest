package com.example.bankcards.service;

import com.example.bankcards.dto.card.CardAdminResponse;
import com.example.bankcards.dto.card.CardCreateRequest;
import com.example.bankcards.dto.card.CardResponse;
import com.example.bankcards.entity.Card;
import com.example.bankcards.entity.CardStatus;
import com.example.bankcards.entity.Person;
import com.example.bankcards.entity.Role;
import org.springframework.util.StringUtils;
import com.example.bankcards.exception.CardNumberGenerationException;
import com.example.bankcards.exception.InvalidCardOperationException;
import com.example.bankcards.exception.ResourceNotFoundException;
import com.example.bankcards.repository.CardRepository;
import com.example.bankcards.repository.PersonRepository;
import com.example.bankcards.util.CardEncryptionUtil;
import com.example.bankcards.util.CardMaskUtil;
import com.example.bankcards.util.CardNumberGenerator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

@Slf4j
@RequiredArgsConstructor
@Service
public class CardService {
    private final CardRepository cardRepository;
    private final PersonRepository personRepository;
    private final CardNumberGenerator cardNumberGenerator;
    private final CardEncryptionUtil cardEncryptionUtil;
    private final CardMaskUtil cardMaskUtil;
    private static final String PERSON_NOT_FOUND = "Person not found with id: %s";
    private static final String CARD_NOT_FOUND = "Card not found with id: %s";

    @Transactional(readOnly = true)
    public Page<CardAdminResponse> getAllCardsAdmin(UUID personId, String status, Pageable pageable) {
        Page<Card> cards;
        if (personId != null && StringUtils.hasText(status)) {
            CardStatus cardStatus = CardStatus.valueOf(status.toUpperCase());
            cards = cardRepository.findByPerson_IdAndCardStatus(personId, cardStatus, pageable);
        } else if (personId != null) {
            cards = cardRepository.findByPerson_Id(personId, pageable);
        } else if (StringUtils.hasText(status)) {
            CardStatus cardStatus = CardStatus.valueOf(status.toUpperCase());
            cards = cardRepository.findByCardStatus(cardStatus, pageable);
        } else {
            cards = cardRepository.findAll(pageable);
        }
        return cards.map(this::toAdminResponse);
    }

    @Transactional(readOnly = true)
    public Page<CardResponse> getCardsByPersonUser(UUID personId, String status, Pageable pageable) {
        return getCardsByPersonInternal(personId, status, pageable)
                .map(this::toUserResponse);
    }

    @Transactional(readOnly = true)
    public CardAdminResponse getCardByIdAdmin(UUID id) {
        Card card = cardRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(String.format(CARD_NOT_FOUND, id)));
        return toAdminResponse(card);
    }

    @Transactional(readOnly = true)
    public CardResponse getCardByIdUser(UUID id) {
        Card card = cardRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(String.format(CARD_NOT_FOUND, id)));
        return toUserResponse(card);
    }

    @Transactional
    public CardAdminResponse createCard(CardCreateRequest request) {
        Person person = personRepository.findById(request.personId())
                .orElseThrow(() -> new ResourceNotFoundException(String.format(PERSON_NOT_FOUND, request.personId())));

        if (person.getRole() == Role.ADMIN) {
            throw new InvalidCardOperationException("Cannot create cards for admin users");
        }

        for (int attempt = 0; attempt < 3; attempt++) {
            try {
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
                log.info("Card created: id={}, personId={}", saved.getId(), person.getId());
                return toAdminResponse(saved);
            } catch (org.springframework.dao.DataIntegrityViolationException e) {
                if (attempt == 2) {
                    throw new CardNumberGenerationException("Failed to generate unique card number after 3 attempts", e);
                }
                log.warn("Card hash collision on attempt {}, retrying", attempt + 1);
            }
        }
        throw new CardNumberGenerationException("Failed to generate unique card number");
    }

    @Transactional
    public CardAdminResponse blockCard(UUID id) {
        Card card = cardRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(String.format(CARD_NOT_FOUND, id)));

        if (card.getCardStatus() != CardStatus.ACTIVE) {
            throw new InvalidCardOperationException("Only active cards can be blocked");
        }

        card.setCardStatus(CardStatus.BLOCKED);
        log.info("Card blocked: id={}", id);
        return toAdminResponse(card);
    }

    @Transactional
    public CardAdminResponse activateCard(UUID id) {
        Card card = cardRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(String.format(CARD_NOT_FOUND, id)));

        if (card.getCardStatus() != CardStatus.BLOCKED) {
            throw new InvalidCardOperationException("Only blocked cards can be activated");
        }

        card.setCardStatus(CardStatus.ACTIVE);
        log.info("Card activated: id={}", id);
        return toAdminResponse(card);
    }

    @Transactional
    public void deleteCard(UUID id) {
        Card card = cardRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(String.format(CARD_NOT_FOUND, id)));
        cardRepository.delete(card);
        log.info("Card deleted: id={}", id);
    }

    private Page<Card> getCardsByPersonInternal(UUID personId, String status, Pageable pageable) {
        if (StringUtils.hasText(status)) {
            CardStatus cardStatus = CardStatus.valueOf(status.toUpperCase());
            return cardRepository.findByPerson_IdAndCardStatus(personId, cardStatus, pageable);
        }
        return cardRepository.findByPerson_Id(personId, pageable);
    }

    private CardAdminResponse toAdminResponse(Card card) {
        String masked = cardMaskUtil.decryptAndMask(card.getEncryptedNumber());
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
        String masked = cardMaskUtil.decryptAndMask(card.getEncryptedNumber());
        return new CardResponse(
                card.getId(),
                masked,
                card.getExpirationDate(),
                card.getCardStatus(),
                card.getBalance()
        );
    }
}
