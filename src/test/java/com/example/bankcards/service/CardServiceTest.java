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
import com.example.bankcards.mapper.CardMapper;
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

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CardServiceTest {

    @Mock private CardRepository cardRepository;
    @Mock private PersonRepository personRepository;
    @Mock private CardMapper cardMapper;
    @Mock private CardNumberGenerator cardNumberGenerator;
    @Mock private CardEncryptionUtil cardEncryptionUtil;
    @Mock private CardMaskUtil cardMaskUtil;

    @InjectMocks
    private CardService cardService;

    @Test
    void getCardsByPersonAdmin_shouldReturnAdminPage() {
        Person person = createPerson(1L);
        Card card = createCard(1L, person, CardStatus.ACTIVE);
        when(cardRepository.findByPerson_Id(eq(1L), any(PageRequest.class)))
                .thenReturn(new PageImpl<>(List.of(card)));
        when(cardEncryptionUtil.decrypt("encrypted")).thenReturn("4000001234567890");
        when(cardMaskUtil.mask("4000001234567890")).thenReturn("**** **** **** 7890");

        Page<CardAdminResponse> result = cardService.getCardsByPersonAdmin(1L, null, PageRequest.of(0, 10));

        assertEquals(1, result.getContent().size());
        assertEquals("**** **** **** 7890", result.getContent().get(0).maskedNumber());
        assertEquals(1L, result.getContent().get(0).id());
    }

    @Test
    void getCardsByPersonAdmin_shouldReturnEmptyPageWhenNoCards() {
        when(cardRepository.findByPerson_Id(eq(1L), any(PageRequest.class)))
                .thenReturn(new PageImpl<>(List.of()));

        Page<CardAdminResponse> result = cardService.getCardsByPersonAdmin(1L, null, PageRequest.of(0, 10));

        assertTrue(result.isEmpty());
    }

    @Test
    void getCardByIdAdmin_shouldReturnCard() {
        Person person = createPerson(1L);
        Card card = createCard(1L, person, CardStatus.ACTIVE);
        when(cardRepository.findById(1L)).thenReturn(Optional.of(card));
        when(cardEncryptionUtil.decrypt("encrypted")).thenReturn("4000001234567890");
        when(cardMaskUtil.mask("4000001234567890")).thenReturn("**** **** **** 7890");

        CardAdminResponse result = cardService.getCardByIdAdmin(1L);

        assertEquals(1L, result.id());
        assertEquals("**** **** **** 7890", result.maskedNumber());
    }

    @Test
    void getCardByIdAdmin_shouldThrowWhenNotFound() {
        when(cardRepository.findById(99L)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> cardService.getCardByIdAdmin(99L));
    }

    @Test
    void createCard_shouldCreateCard() {
        Person person = createPerson(1L);
        Card card = createCard(1L, person, CardStatus.ACTIVE);
        when(personRepository.findById(1L)).thenReturn(Optional.of(person));
        when(cardNumberGenerator.generate()).thenReturn("4000001234567890");
        when(cardEncryptionUtil.encrypt("4000001234567890")).thenReturn("encrypted");
        when(cardEncryptionUtil.hash("4000001234567890")).thenReturn("hash");
        when(cardRepository.save(any(Card.class))).thenReturn(card);
        when(cardEncryptionUtil.decrypt("encrypted")).thenReturn("4000001234567890");
        when(cardMaskUtil.mask("4000001234567890")).thenReturn("**** **** **** 7890");

        CardAdminResponse result = cardService.createCard(new CardCreateRequest(1L, LocalDate.now().plusYears(1), BigDecimal.valueOf(100)));

        assertNotNull(result);
        assertEquals(CardStatus.ACTIVE, result.cardStatus());
        verify(cardRepository).save(any(Card.class));
    }

    @Test
    void createCard_shouldThrowWhenPersonNotFound() {
        when(personRepository.findById(99L)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> cardService.createCard(new CardCreateRequest(99L, LocalDate.now().plusYears(1), BigDecimal.ZERO)));
    }

    @Test
    void createCard_shouldThrowWhenExpirationInPast() {
        Person person = createPerson(1L);
        when(personRepository.findById(1L)).thenReturn(Optional.of(person));
        assertThrows(InvalidCardOperationException.class, () -> cardService.createCard(new CardCreateRequest(1L, LocalDate.now().minusDays(1), BigDecimal.ZERO)));
    }

    @Test
    void blockCard_shouldBlockActiveCard() {
        Person person = createPerson(1L);
        Card card = createCard(1L, person, CardStatus.ACTIVE);
        when(cardRepository.findById(1L)).thenReturn(Optional.of(card));
        when(cardEncryptionUtil.decrypt("encrypted")).thenReturn("4000001234567890");
        when(cardMaskUtil.mask("4000001234567890")).thenReturn("**** **** **** 7890");

        CardAdminResponse result = cardService.blockCard(1L);

        assertNotNull(result);
        assertEquals(CardStatus.BLOCKED, card.getCardStatus());
    }

    @Test
    void blockCard_shouldThrowWhenAlreadyBlocked() {
        Person person = createPerson(1L);
        Card card = createCard(1L, person, CardStatus.BLOCKED);
        when(cardRepository.findById(1L)).thenReturn(Optional.of(card));
        assertThrows(InvalidCardOperationException.class, () -> cardService.blockCard(1L));
    }

    @Test
    void blockCard_shouldThrowWhenNotFound() {
        when(cardRepository.findById(99L)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> cardService.blockCard(99L));
    }

    @Test
    void activateCard_shouldActivateBlockedCard() {
        Person person = createPerson(1L);
        Card card = createCard(1L, person, CardStatus.BLOCKED);
        when(cardRepository.findById(1L)).thenReturn(Optional.of(card));
        when(cardEncryptionUtil.decrypt("encrypted")).thenReturn("4000001234567890");
        when(cardMaskUtil.mask("4000001234567890")).thenReturn("**** **** **** 7890");

        CardAdminResponse result = cardService.activateCard(1L);

        assertNotNull(result);
        assertEquals(CardStatus.ACTIVE, card.getCardStatus());
    }

    @Test
    void activateCard_shouldThrowWhenNotBlocked() {
        Person person = createPerson(1L);
        Card card = createCard(1L, person, CardStatus.ACTIVE);
        when(cardRepository.findById(1L)).thenReturn(Optional.of(card));
        assertThrows(InvalidCardOperationException.class, () -> cardService.activateCard(1L));
    }

    @Test
    void deleteCard_shouldDeleteCard() {
        Person person = createPerson(1L);
        Card card = createCard(1L, person, CardStatus.ACTIVE);
        when(cardRepository.findById(1L)).thenReturn(Optional.of(card));
        cardService.deleteCard(1L);
        verify(cardRepository).delete(card);
    }

    @Test
    void deleteCard_shouldThrowWhenNotFound() {
        when(cardRepository.findById(99L)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> cardService.deleteCard(99L));
    }

    private Person createPerson(Long id) {
        Person p = new Person();
        p.setId(id);
        p.setName("Alice");
        p.setPassword("pass");
        p.setRole(Role.USER);
        return p;
    }

    private Card createCard(Long id, Person person, CardStatus status) {
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
