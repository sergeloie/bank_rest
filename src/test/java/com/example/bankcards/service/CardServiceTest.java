package com.example.bankcards.service;

import com.example.bankcards.dto.card.CardAdminResponse;
import com.example.bankcards.dto.card.CardCreateRequest;
import com.example.bankcards.dto.card.CardResponse;
import com.example.bankcards.entity.Card;
import com.example.bankcards.entity.CardStatus;
import com.example.bankcards.entity.Person;
import com.example.bankcards.entity.Role;
import com.example.bankcards.exception.InvalidCardOperationException;
import com.example.bankcards.exception.ResourceNotFoundException;
import com.example.bankcards.repository.CardRepository;
import com.example.bankcards.repository.PersonRepository;
import com.example.bankcards.util.CardEncryptionUtil;
import com.example.bankcards.util.CardMaskUtil;
import com.example.bankcards.util.CardNumberGenerator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CardServiceTest {

    @Mock private CardRepository cardRepository;
    @Mock private PersonRepository personRepository;
    @Mock private CardNumberGenerator cardNumberGenerator;
    @Mock private CardEncryptionUtil cardEncryptionUtil;
    @Mock private CardMaskUtil cardMaskUtil;

    @InjectMocks
    private CardService cardService;

    @Test
    void getCardByIdAdmin_shouldReturnCard() {
        UUID personId = UUID.randomUUID();
        UUID cardId = UUID.randomUUID();
        Person person = createPerson(personId);
        Card card = createCard(cardId, person, CardStatus.ACTIVE);
        when(cardRepository.findById(cardId)).thenReturn(Optional.of(card));
        when(cardEncryptionUtil.decrypt("encrypted")).thenReturn("4000001234567890");
        when(cardMaskUtil.mask("4000001234567890")).thenReturn("**** **** **** 7890");

        CardAdminResponse result = cardService.getCardByIdAdmin(cardId);

        assertEquals(cardId, result.id());
        assertEquals("**** **** **** 7890", result.maskedNumber());
    }

    @Test
    void getCardByIdAdmin_shouldThrowWhenNotFound() {
        UUID id = UUID.randomUUID();
        when(cardRepository.findById(id)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> cardService.getCardByIdAdmin(id));
    }

    @Test
    void createCard_shouldCreateCard() {
        UUID personId = UUID.randomUUID();
        UUID cardId = UUID.randomUUID();
        Person person = createPerson(personId);
        Card card = createCard(cardId, person, CardStatus.ACTIVE);
        when(personRepository.findById(personId)).thenReturn(Optional.of(person));
        when(cardNumberGenerator.generate()).thenReturn("4000001234567890");
        when(cardEncryptionUtil.encrypt("4000001234567890")).thenReturn("encrypted");
        when(cardEncryptionUtil.hash("4000001234567890")).thenReturn("hash");
        when(cardRepository.save(any(Card.class))).thenReturn(card);
        when(cardEncryptionUtil.decrypt("encrypted")).thenReturn("4000001234567890");
        when(cardMaskUtil.mask("4000001234567890")).thenReturn("**** **** **** 7890");

        CardAdminResponse result = cardService.createCard(new CardCreateRequest(personId, LocalDate.now().plusYears(1), BigDecimal.valueOf(100)));

        assertNotNull(result);
        assertEquals(CardStatus.ACTIVE, result.cardStatus());
        verify(cardRepository).save(any(Card.class));
    }

    @Test
    void createCard_shouldThrowWhenPersonNotFound() {
        UUID personId = UUID.randomUUID();
        when(personRepository.findById(personId)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> cardService.createCard(new CardCreateRequest(personId, LocalDate.now().plusYears(1), BigDecimal.ZERO)));
    }

    @Test
    void createCard_shouldThrowWhenPersonIsAdmin() {
        UUID personId = UUID.randomUUID();
        Person person = createPerson(personId);
        person.setRole(Role.ADMIN);
        when(personRepository.findById(personId)).thenReturn(Optional.of(person));
        assertThrows(InvalidCardOperationException.class, () -> cardService.createCard(new CardCreateRequest(personId, LocalDate.now().plusYears(1), BigDecimal.ZERO)));
    }

    @Test
    void blockCard_shouldBlockActiveCard() {
        UUID personId = UUID.randomUUID();
        UUID cardId = UUID.randomUUID();
        Person person = createPerson(personId);
        Card card = createCard(cardId, person, CardStatus.ACTIVE);
        when(cardRepository.findById(cardId)).thenReturn(Optional.of(card));
        when(cardEncryptionUtil.decrypt("encrypted")).thenReturn("4000001234567890");
        when(cardMaskUtil.mask("4000001234567890")).thenReturn("**** **** **** 7890");

        CardAdminResponse result = cardService.blockCard(cardId);

        assertNotNull(result);
        assertEquals(CardStatus.BLOCKED, card.getCardStatus());
    }

    @Test
    void blockCard_shouldThrowWhenAlreadyBlocked() {
        UUID personId = UUID.randomUUID();
        UUID cardId = UUID.randomUUID();
        Person person = createPerson(personId);
        Card card = createCard(cardId, person, CardStatus.BLOCKED);
        when(cardRepository.findById(cardId)).thenReturn(Optional.of(card));
        assertThrows(InvalidCardOperationException.class, () -> cardService.blockCard(cardId));
    }

    @Test
    void blockCard_shouldThrowWhenNotFound() {
        UUID id = UUID.randomUUID();
        when(cardRepository.findById(id)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> cardService.blockCard(id));
    }

    @Test
    void activateCard_shouldActivateBlockedCard() {
        UUID personId = UUID.randomUUID();
        UUID cardId = UUID.randomUUID();
        Person person = createPerson(personId);
        Card card = createCard(cardId, person, CardStatus.BLOCKED);
        when(cardRepository.findById(cardId)).thenReturn(Optional.of(card));
        when(cardEncryptionUtil.decrypt("encrypted")).thenReturn("4000001234567890");
        when(cardMaskUtil.mask("4000001234567890")).thenReturn("**** **** **** 7890");

        CardAdminResponse result = cardService.activateCard(cardId);

        assertNotNull(result);
        assertEquals(CardStatus.ACTIVE, card.getCardStatus());
    }

    @Test
    void activateCard_shouldThrowWhenNotBlocked() {
        UUID personId = UUID.randomUUID();
        UUID cardId = UUID.randomUUID();
        Person person = createPerson(personId);
        Card card = createCard(cardId, person, CardStatus.ACTIVE);
        when(cardRepository.findById(cardId)).thenReturn(Optional.of(card));
        assertThrows(InvalidCardOperationException.class, () -> cardService.activateCard(cardId));
    }

    @Test
    void deleteCard_shouldDeleteCard() {
        UUID personId = UUID.randomUUID();
        UUID cardId = UUID.randomUUID();
        Person person = createPerson(personId);
        Card card = createCard(cardId, person, CardStatus.ACTIVE);
        when(cardRepository.findById(cardId)).thenReturn(Optional.of(card));
        cardService.deleteCard(cardId);
        verify(cardRepository).delete(card);
    }

    @Test
    void deleteCard_shouldThrowWhenNotFound() {
        UUID id = UUID.randomUUID();
        when(cardRepository.findById(id)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> cardService.deleteCard(id));
    }

    private Person createPerson(UUID id) {
        Person p = new Person();
        p.setId(id);
        p.setName("Alice");
        p.setPassword("pass");
        p.setRole(Role.USER);
        return p;
    }

    private Card createCard(UUID id, Person person, CardStatus status) {
        Card c = new Card();
        c.setId(id);
        c.setPerson(person);
        c.setEncryptedNumber("encrypted");
        c.setExpirationDate(LocalDate.now().plusYears(1));
        c.setCardStatus(status);
        c.setBalance(BigDecimal.valueOf(100));
        return c;
    }
}
