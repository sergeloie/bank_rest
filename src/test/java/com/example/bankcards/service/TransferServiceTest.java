package com.example.bankcards.service;

import com.example.bankcards.dto.card.CardTransferRequest;
import com.example.bankcards.dto.card.CardTransferResponse;
import com.example.bankcards.entity.Card;
import com.example.bankcards.entity.CardStatus;
import com.example.bankcards.entity.Person;
import com.example.bankcards.entity.Role;
import com.example.bankcards.exception.InvalidCardOperationException;
import com.example.bankcards.exception.ResourceNotFoundException;
import com.example.bankcards.repository.CardRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TransferServiceTest {

    @Mock
    private CardRepository cardRepository;

    @InjectMocks
    private TransferService transferService;

    @Test
    void transfer_shouldTransferBetweenCards() {
        Person person = createPerson(1L);
        Card fromCard = createCard(1L, person, CardStatus.ACTIVE, BigDecimal.valueOf(500));
        Card toCard = createCard(2L, person, CardStatus.ACTIVE, BigDecimal.valueOf(100));
        CardTransferRequest request = new CardTransferRequest(1L, 2L, BigDecimal.valueOf(200), 1L);

        when(cardRepository.findById(1L)).thenReturn(Optional.of(fromCard));
        when(cardRepository.findById(2L)).thenReturn(Optional.of(toCard));

        CardTransferResponse result = transferService.transfer(request);

        assertEquals(BigDecimal.valueOf(200), result.amount());
        assertEquals(BigDecimal.valueOf(300), result.newFromBalance());
        assertEquals(BigDecimal.valueOf(300), result.newToBalance());
    }

    @Test
    void transfer_shouldThrowWhenSourceCardNotFound() {
        CardTransferRequest request = new CardTransferRequest(99L, 2L, BigDecimal.valueOf(100), 1L);
        when(cardRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> transferService.transfer(request));
    }

    @Test
    void transfer_shouldThrowWhenTargetCardNotFound() {
        Person person = createPerson(1L);
        Card fromCard = createCard(1L, person, CardStatus.ACTIVE, BigDecimal.valueOf(500));
        CardTransferRequest request = new CardTransferRequest(1L, 99L, BigDecimal.valueOf(100), 1L);
        when(cardRepository.findById(1L)).thenReturn(Optional.of(fromCard));
        when(cardRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> transferService.transfer(request));
    }

    @Test
    void transfer_shouldThrowWhenSameCard() {
        Person person = createPerson(1L);
        Card card = createCard(1L, person, CardStatus.ACTIVE, BigDecimal.valueOf(500));
        CardTransferRequest request = new CardTransferRequest(1L, 1L, BigDecimal.valueOf(100), 1L);
        when(cardRepository.findById(1L)).thenReturn(Optional.of(card));

        assertThrows(InvalidCardOperationException.class, () -> transferService.transfer(request));
    }

    @Test
    void transfer_shouldThrowWhenDifferentOwners() {
        Person person1 = createPerson(1L);
        Person person2 = createPerson(2L);
        Card fromCard = createCard(1L, person1, CardStatus.ACTIVE, BigDecimal.valueOf(500));
        Card toCard = createCard(2L, person2, CardStatus.ACTIVE, BigDecimal.valueOf(100));
        CardTransferRequest request = new CardTransferRequest(1L, 2L, BigDecimal.valueOf(100), 1L);
        when(cardRepository.findById(1L)).thenReturn(Optional.of(fromCard));
        when(cardRepository.findById(2L)).thenReturn(Optional.of(toCard));

        assertThrows(InvalidCardOperationException.class, () -> transferService.transfer(request));
    }

    @Test
    void transfer_shouldThrowWhenPersonMismatch() {
        Person person1 = createPerson(1L);
        Person person2 = createPerson(2L);
        Card fromCard = createCard(1L, person1, CardStatus.ACTIVE, BigDecimal.valueOf(500));
        Card toCard = createCard(2L, person2, CardStatus.ACTIVE, BigDecimal.valueOf(100));
        CardTransferRequest request = new CardTransferRequest(1L, 2L, BigDecimal.valueOf(100), 99L);
        when(cardRepository.findById(1L)).thenReturn(Optional.of(fromCard));
        when(cardRepository.findById(2L)).thenReturn(Optional.of(toCard));

        assertThrows(InvalidCardOperationException.class, () -> transferService.transfer(request));
    }

    @Test
    void transfer_shouldThrowWhenSourceCardBlocked() {
        Person person = createPerson(1L);
        Card fromCard = createCard(1L, person, CardStatus.BLOCKED, BigDecimal.valueOf(500));
        Card toCard = createCard(2L, person, CardStatus.ACTIVE, BigDecimal.valueOf(100));
        CardTransferRequest request = new CardTransferRequest(1L, 2L, BigDecimal.valueOf(100), 1L);
        when(cardRepository.findById(1L)).thenReturn(Optional.of(fromCard));
        when(cardRepository.findById(2L)).thenReturn(Optional.of(toCard));

        assertThrows(InvalidCardOperationException.class, () -> transferService.transfer(request));
    }

    @Test
    void transfer_shouldThrowWhenTargetCardBlocked() {
        Person person = createPerson(1L);
        Card fromCard = createCard(1L, person, CardStatus.ACTIVE, BigDecimal.valueOf(500));
        Card toCard = createCard(2L, person, CardStatus.BLOCKED, BigDecimal.valueOf(100));
        CardTransferRequest request = new CardTransferRequest(1L, 2L, BigDecimal.valueOf(100), 1L);
        when(cardRepository.findById(1L)).thenReturn(Optional.of(fromCard));
        when(cardRepository.findById(2L)).thenReturn(Optional.of(toCard));

        assertThrows(InvalidCardOperationException.class, () -> transferService.transfer(request));
    }

    @Test
    void transfer_shouldThrowWhenInsufficientFunds() {
        Person person = createPerson(1L);
        Card fromCard = createCard(1L, person, CardStatus.ACTIVE, BigDecimal.valueOf(50));
        Card toCard = createCard(2L, person, CardStatus.ACTIVE, BigDecimal.valueOf(100));
        CardTransferRequest request = new CardTransferRequest(1L, 2L, BigDecimal.valueOf(100), 1L);
        when(cardRepository.findById(1L)).thenReturn(Optional.of(fromCard));
        when(cardRepository.findById(2L)).thenReturn(Optional.of(toCard));

        assertThrows(InvalidCardOperationException.class, () -> transferService.transfer(request));
    }

    private Person createPerson(Long id) {
        Person p = new Person();
        p.setId(id);
        p.setName("Alice");
        p.setPassword("pass");
        p.setRole(Role.USER);
        return p;
    }

    private Card createCard(Long id, Person person, CardStatus status, BigDecimal balance) {
        Card c = new Card();
        c.setId(id);
        c.setPerson(person);
        c.setCardStatus(status);
        c.setBalance(balance);
        return c;
    }
}
