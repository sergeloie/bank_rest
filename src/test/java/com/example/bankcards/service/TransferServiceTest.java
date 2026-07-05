package com.example.bankcards.service;

import com.example.bankcards.dto.card.CardTransferRequest;
import com.example.bankcards.dto.card.CardTransferResponse;
import com.example.bankcards.entity.Card;
import com.example.bankcards.entity.CardStatus;
import com.example.bankcards.entity.Person;
import com.example.bankcards.entity.Role;
import com.example.bankcards.entity.TransferHistory;
import com.example.bankcards.exception.InvalidCardOperationException;
import com.example.bankcards.exception.ResourceNotFoundException;
import com.example.bankcards.repository.CardRepository;
import com.example.bankcards.repository.TransferHistoryRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TransferServiceTest {

    @Mock
    private CardRepository cardRepository;

    @Mock
    private TransferHistoryRepository transferHistoryRepository;

    @InjectMocks
    private TransferService transferService;

    @Test
    void transfer_shouldTransferBetweenCards() {
        UUID personId = UUID.randomUUID();
        UUID fromCardId = UUID.randomUUID();
        UUID toCardId = UUID.randomUUID();
        Person person = createPerson(personId);
        Card fromCard = createCard(fromCardId, person, CardStatus.ACTIVE, BigDecimal.valueOf(500));
        Card toCard = createCard(toCardId, person, CardStatus.ACTIVE, BigDecimal.valueOf(100));
        CardTransferRequest request = new CardTransferRequest(fromCardId, toCardId, BigDecimal.valueOf(200));

        when(cardRepository.findByIdForUpdate(fromCardId)).thenReturn(Optional.of(fromCard));
        when(cardRepository.findByIdForUpdate(toCardId)).thenReturn(Optional.of(toCard));

        CardTransferResponse result = transferService.transfer(request, personId);

        assertEquals(BigDecimal.valueOf(200), result.amount());
        assertEquals(BigDecimal.valueOf(300), result.newFromBalance());
        assertEquals(BigDecimal.valueOf(300), result.newToBalance());
        verify(transferHistoryRepository).save(any(TransferHistory.class));
    }

    @Test
    void transfer_shouldThrowWhenSameCard() {
        UUID personId = UUID.randomUUID();
        UUID cardId = UUID.randomUUID();
        CardTransferRequest request = new CardTransferRequest(cardId, cardId, BigDecimal.valueOf(100));

        assertThrows(InvalidCardOperationException.class, () -> transferService.transfer(request, personId));
        verifyNoInteractions(cardRepository);
    }

    @Test
    void transfer_shouldThrowWhenSourceCardNotFound() {
        UUID personId = UUID.randomUUID();
        UUID fromCardId = UUID.fromString("00000000-0000-0000-0000-000000000001");
        UUID toCardId = UUID.fromString("ffffffff-ffff-ffff-ffff-ffffffffffff");
        lenient().when(cardRepository.findByIdForUpdate(fromCardId)).thenReturn(Optional.empty());
        lenient().when(cardRepository.findByIdForUpdate(toCardId)).thenReturn(Optional.empty());

        CardTransferRequest request = new CardTransferRequest(fromCardId, toCardId, BigDecimal.valueOf(100));
        assertThrows(ResourceNotFoundException.class, () -> transferService.transfer(request, personId));
    }

    @Test
    void transfer_shouldThrowWhenTargetCardNotFound() {
        UUID personId = UUID.randomUUID();
        UUID fromCardId = UUID.fromString("00000000-0000-0000-0000-000000000001");
        UUID toCardId = UUID.fromString("ffffffff-ffff-ffff-ffff-ffffffffffff");
        Person person = createPerson(personId);
        Card fromCard = createCard(fromCardId, person, CardStatus.ACTIVE, BigDecimal.valueOf(500));
        lenient().when(cardRepository.findByIdForUpdate(fromCardId)).thenReturn(Optional.of(fromCard));
        lenient().when(cardRepository.findByIdForUpdate(toCardId)).thenReturn(Optional.empty());

        CardTransferRequest request = new CardTransferRequest(fromCardId, toCardId, BigDecimal.valueOf(100));
        assertThrows(ResourceNotFoundException.class, () -> transferService.transfer(request, personId));
    }

    @Test
    void transfer_shouldThrowWhenDifferentOwners() {
        UUID personId1 = UUID.randomUUID();
        UUID personId2 = UUID.randomUUID();
        UUID fromCardId = UUID.randomUUID();
        UUID toCardId = UUID.randomUUID();
        Person person1 = createPerson(personId1);
        Person person2 = createPerson(personId2);
        Card fromCard = createCard(fromCardId, person1, CardStatus.ACTIVE, BigDecimal.valueOf(500));
        Card toCard = createCard(toCardId, person2, CardStatus.ACTIVE, BigDecimal.valueOf(100));
        when(cardRepository.findByIdForUpdate(fromCardId)).thenReturn(Optional.of(fromCard));
        when(cardRepository.findByIdForUpdate(toCardId)).thenReturn(Optional.of(toCard));

        CardTransferRequest request = new CardTransferRequest(fromCardId, toCardId, BigDecimal.valueOf(100));
        assertThrows(InvalidCardOperationException.class, () -> transferService.transfer(request, personId1));
    }

    @Test
    void transfer_shouldThrowWhenSourceCardBlocked() {
        UUID personId = UUID.randomUUID();
        UUID fromCardId = UUID.randomUUID();
        UUID toCardId = UUID.randomUUID();
        Person person = createPerson(personId);
        Card fromCard = createCard(fromCardId, person, CardStatus.BLOCKED, BigDecimal.valueOf(500));
        Card toCard = createCard(toCardId, person, CardStatus.ACTIVE, BigDecimal.valueOf(100));
        when(cardRepository.findByIdForUpdate(fromCardId)).thenReturn(Optional.of(fromCard));
        when(cardRepository.findByIdForUpdate(toCardId)).thenReturn(Optional.of(toCard));

        CardTransferRequest request = new CardTransferRequest(fromCardId, toCardId, BigDecimal.valueOf(100));
        assertThrows(InvalidCardOperationException.class, () -> transferService.transfer(request, personId));
    }

    @Test
    void transfer_shouldThrowWhenTargetCardBlocked() {
        UUID personId = UUID.randomUUID();
        UUID fromCardId = UUID.randomUUID();
        UUID toCardId = UUID.randomUUID();
        Person person = createPerson(personId);
        Card fromCard = createCard(fromCardId, person, CardStatus.ACTIVE, BigDecimal.valueOf(500));
        Card toCard = createCard(toCardId, person, CardStatus.BLOCKED, BigDecimal.valueOf(100));
        when(cardRepository.findByIdForUpdate(fromCardId)).thenReturn(Optional.of(fromCard));
        when(cardRepository.findByIdForUpdate(toCardId)).thenReturn(Optional.of(toCard));

        CardTransferRequest request = new CardTransferRequest(fromCardId, toCardId, BigDecimal.valueOf(100));
        assertThrows(InvalidCardOperationException.class, () -> transferService.transfer(request, personId));
    }

    @Test
    void transfer_shouldThrowWhenInsufficientFunds() {
        UUID personId = UUID.randomUUID();
        UUID fromCardId = UUID.randomUUID();
        UUID toCardId = UUID.randomUUID();
        Person person = createPerson(personId);
        Card fromCard = createCard(fromCardId, person, CardStatus.ACTIVE, BigDecimal.valueOf(50));
        Card toCard = createCard(toCardId, person, CardStatus.ACTIVE, BigDecimal.valueOf(100));
        when(cardRepository.findByIdForUpdate(fromCardId)).thenReturn(Optional.of(fromCard));
        when(cardRepository.findByIdForUpdate(toCardId)).thenReturn(Optional.of(toCard));

        CardTransferRequest request = new CardTransferRequest(fromCardId, toCardId, BigDecimal.valueOf(100));
        assertThrows(InvalidCardOperationException.class, () -> transferService.transfer(request, personId));
    }

    private Person createPerson(UUID id) {
        Person p = new Person();
        p.setId(id);
        p.setName("Alice");
        p.setPassword("pass");
        p.setRole(Role.USER);
        return p;
    }

    private Card createCard(UUID id, Person person, CardStatus status, BigDecimal balance) {
        Card c = new Card();
        c.setId(id);
        c.setPerson(person);
        c.setCardStatus(status);
        c.setBalance(balance);
        return c;
    }
}
