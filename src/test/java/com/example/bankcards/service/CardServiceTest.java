package com.example.bankcards.service;

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
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CardServiceTest {

    @Mock
    private CardRepository cardRepository;

    @Mock
    private PersonRepository personRepository;

    @Mock
    private CardMapper cardMapper;

    @Mock
    private CardNumberGenerator cardNumberGenerator;

    @Mock
    private CardEncryptionUtil cardEncryptionUtil;

    @InjectMocks
    private CardService cardService;

    @Test
    void getCardsByPerson_shouldReturnPage() {
        Person person = createPerson(1L);
        Card card = createCard(1L, person, CardStatus.ACTIVE);
        CardResponse dto = createCardResponse(1L);
        when(personRepository.existsById(1L)).thenReturn(true);
        when(cardRepository.findByPerson_Id(eq(1L), any(PageRequest.class)))
                .thenReturn(new PageImpl<>(List.of(card)));
        when(cardMapper.toCardResponse(card)).thenReturn(dto);

        Page<CardResponse> result = cardService.getCardsByPerson(1L, PageRequest.of(0, 10));

        assertEquals(1, result.getContent().size());
    }

    @Test
    void getCardsByPerson_shouldThrowWhenPersonNotFound() {
        when(personRepository.existsById(99L)).thenReturn(false);

        assertThrows(ResourceNotFoundException.class,
                () -> cardService.getCardsByPerson(99L, PageRequest.of(0, 10)));
    }

    @Test
    void getCardById_shouldReturnCard() {
        Person person = createPerson(1L);
        Card card = createCard(1L, person, CardStatus.ACTIVE);
        CardResponse dto = createCardResponse(1L);
        when(cardRepository.findById(1L)).thenReturn(Optional.of(card));
        when(cardMapper.toCardResponse(card)).thenReturn(dto);

        CardResponse result = cardService.getCardById(1L);

        assertEquals(1L, result.id());
    }

    @Test
    void getCardById_shouldThrowWhenNotFound() {
        when(cardRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> cardService.getCardById(99L));
    }

    @Test
    void createCard_shouldCreateCard() {
        Person person = createPerson(1L);
        CardCreateRequest request = new CardCreateRequest(1L, LocalDate.now().plusYears(1), BigDecimal.valueOf(100));
        Card card = createCard(1L, person, CardStatus.ACTIVE);
        CardResponse dto = createCardResponse(1L);
        when(personRepository.findById(1L)).thenReturn(Optional.of(person));
        when(cardNumberGenerator.generate()).thenReturn("4000001234567890");
        when(cardEncryptionUtil.encrypt("4000001234567890")).thenReturn("encrypted");
        when(cardRepository.save(any(Card.class))).thenReturn(card);
        when(cardMapper.toCardResponse(any(Card.class))).thenReturn(dto);

        CardResponse result = cardService.createCard(request);

        assertNotNull(result);
        verify(cardRepository).save(any(Card.class));
    }

    @Test
    void createCard_shouldThrowWhenPersonNotFound() {
        CardCreateRequest request = new CardCreateRequest(99L, LocalDate.now().plusYears(1), BigDecimal.ZERO);
        when(personRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> cardService.createCard(request));
    }

    @Test
    void createCard_shouldThrowWhenExpirationInPast() {
        Person person = createPerson(1L);
        CardCreateRequest request = new CardCreateRequest(1L, LocalDate.now().minusDays(1), BigDecimal.ZERO);
        when(personRepository.findById(1L)).thenReturn(Optional.of(person));

        assertThrows(InvalidCardOperationException.class, () -> cardService.createCard(request));
    }

    @Test
    void blockCard_shouldBlockActiveCard() {
        Person person = createPerson(1L);
        Card card = createCard(1L, person, CardStatus.ACTIVE);
        CardResponse dto = createCardResponse(1L);
        when(cardRepository.findById(1L)).thenReturn(Optional.of(card));
        when(cardRepository.save(any(Card.class))).thenReturn(card);
        when(cardMapper.toCardResponse(any(Card.class))).thenReturn(dto);

        CardResponse result = cardService.blockCard(1L);

        assertNotNull(result);
        verify(cardRepository).save(any(Card.class));
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
        CardResponse dto = createCardResponse(1L);
        when(cardRepository.findById(1L)).thenReturn(Optional.of(card));
        when(cardRepository.save(any(Card.class))).thenReturn(card);
        when(cardMapper.toCardResponse(any(Card.class))).thenReturn(dto);

        CardResponse result = cardService.activateCard(1L);

        assertNotNull(result);
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

    private CardResponse createCardResponse(Long id) {
        return new CardResponse(id, 1L, "Alice", LocalDate.now().plusYears(1), CardStatus.ACTIVE, BigDecimal.valueOf(100), null, null);
    }
}
