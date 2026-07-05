package com.example.bankcards.service;

import com.example.bankcards.dto.cardblockrequest.CardBlockRequestAdminResponse;
import com.example.bankcards.dto.cardblockrequest.CardBlockRequestRequest;
import com.example.bankcards.dto.cardblockrequest.CardBlockRequestResponse;
import com.example.bankcards.entity.BlockRequestStatus;
import com.example.bankcards.entity.Card;
import com.example.bankcards.entity.CardBlockRequest;
import com.example.bankcards.entity.CardStatus;
import com.example.bankcards.entity.Person;
import com.example.bankcards.entity.Role;
import com.example.bankcards.exception.InvalidCardOperationException;
import com.example.bankcards.exception.ResourceNotFoundException;
import com.example.bankcards.mapper.CardBlockRequestMapper;
import com.example.bankcards.repository.CardBlockRequestRepository;
import com.example.bankcards.repository.CardRepository;
import com.example.bankcards.repository.PersonRepository;
import com.example.bankcards.util.CardEncryptionUtil;
import com.example.bankcards.util.CardMaskUtil;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CardBlockRequestServiceTest {

    @Mock private CardBlockRequestRepository cardBlockRequestRepository;
    @Mock private CardRepository cardRepository;
    @Mock private PersonRepository personRepository;
    @Mock private CardBlockRequestMapper cardBlockRequestMapper;
    @Mock private CardEncryptionUtil cardEncryptionUtil;
    @Mock private CardMaskUtil cardMaskUtil;

    @InjectMocks
    private CardBlockRequestService cardBlockRequestService;

    @Test
    void createRequest_shouldCreateBlockRequest() {
        UUID personId = UUID.randomUUID();
        UUID cardId = UUID.randomUUID();
        Person person = createPerson(personId);
        Card card = createCard(cardId, person, CardStatus.ACTIVE);
        CardBlockRequestRequest request = new CardBlockRequestRequest(cardId, personId);
        CardBlockRequest blockRequest = createBlockRequest(1L, card, person, BlockRequestStatus.PENDING);

        when(cardRepository.findById(cardId)).thenReturn(Optional.of(card));
        when(personRepository.findById(personId)).thenReturn(Optional.of(person));
        when(cardBlockRequestMapper.toEntity(request)).thenReturn(blockRequest);
        when(cardBlockRequestRepository.save(any(CardBlockRequest.class))).thenReturn(blockRequest);
        when(cardMaskUtil.decryptAndMask("encrypted")).thenReturn("**** **** **** 7890");

        CardBlockRequestResponse result = cardBlockRequestService.createRequest(request);

        assertEquals(BlockRequestStatus.PENDING, result.blockRequestStatus());
        assertEquals("**** **** **** 7890", result.maskedCardNumber());
        verify(cardBlockRequestRepository).save(any(CardBlockRequest.class));
    }

    @Test
    void createRequest_shouldThrowWhenCardNotFound() {
        UUID cardId = UUID.randomUUID();
        UUID personId = UUID.randomUUID();
        CardBlockRequestRequest request = new CardBlockRequestRequest(cardId, personId);
        when(cardRepository.findById(cardId)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> cardBlockRequestService.createRequest(request));
    }

    @Test
    void createRequest_shouldThrowWhenPersonNotFound() {
        UUID personId = UUID.randomUUID();
        UUID cardId = UUID.randomUUID();
        Person person = createPerson(personId);
        Card card = createCard(cardId, person, CardStatus.ACTIVE);
        CardBlockRequestRequest request = new CardBlockRequestRequest(cardId, personId);
        when(cardRepository.findById(cardId)).thenReturn(Optional.of(card));
        when(personRepository.findById(personId)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> cardBlockRequestService.createRequest(request));
    }

    @Test
    void createRequest_shouldThrowWhenCardDoesNotBelongToPerson() {
        UUID personId1 = UUID.randomUUID();
        UUID personId2 = UUID.randomUUID();
        UUID cardId = UUID.randomUUID();
        Person person1 = createPerson(personId1);
        Person person2 = createPerson(personId2);
        Card card = createCard(cardId, person1, CardStatus.ACTIVE);
        CardBlockRequestRequest request = new CardBlockRequestRequest(cardId, personId2);
        when(cardRepository.findById(cardId)).thenReturn(Optional.of(card));
        when(personRepository.findById(personId2)).thenReturn(Optional.of(person2));
        assertThrows(InvalidCardOperationException.class, () -> cardBlockRequestService.createRequest(request));
    }

    @Test
    void createRequest_shouldThrowWhenCardNotActive() {
        UUID personId = UUID.randomUUID();
        UUID cardId = UUID.randomUUID();
        Person person = createPerson(personId);
        Card card = createCard(cardId, person, CardStatus.BLOCKED);
        CardBlockRequestRequest request = new CardBlockRequestRequest(cardId, personId);
        when(cardRepository.findById(cardId)).thenReturn(Optional.of(card));
        when(personRepository.findById(personId)).thenReturn(Optional.of(person));
        assertThrows(InvalidCardOperationException.class, () -> cardBlockRequestService.createRequest(request));
    }

    @Test
    void getPendingRequests_shouldReturnPage() {
        UUID personId = UUID.randomUUID();
        UUID cardId = UUID.randomUUID();
        Person person = createPerson(personId);
        Card card = createCard(cardId, person, CardStatus.ACTIVE);
        CardBlockRequest blockRequest = createBlockRequest(1L, card, person, BlockRequestStatus.PENDING);
        when(cardBlockRequestRepository.findByBlockRequestStatus(eq(BlockRequestStatus.PENDING), any(PageRequest.class)))
                .thenReturn(new PageImpl<>(List.of(blockRequest)));
        when(cardMaskUtil.decryptAndMask("encrypted")).thenReturn("**** **** **** 7890");

        Page<CardBlockRequestAdminResponse> result = cardBlockRequestService.getPendingRequests(PageRequest.of(0, 10));

        assertEquals(1, result.getContent().size());
        assertEquals("**** **** **** 7890", result.getContent().get(0).maskedCardNumber());
    }

    @Test
    void approveRequest_shouldApprovePendingRequest() {
        UUID personId = UUID.randomUUID();
        UUID cardId = UUID.randomUUID();
        Person person = createPerson(personId);
        Card card = createCard(cardId, person, CardStatus.ACTIVE);
        CardBlockRequest blockRequest = createBlockRequest(1L, card, person, BlockRequestStatus.PENDING);
        when(cardBlockRequestRepository.findById(1L)).thenReturn(Optional.of(blockRequest));
        when(cardMaskUtil.decryptAndMask("encrypted")).thenReturn("**** **** **** 7890");

        CardBlockRequestAdminResponse result = cardBlockRequestService.approveRequest(1L);

        assertEquals(BlockRequestStatus.APPROVED, result.blockRequestStatus());
        assertEquals(CardStatus.BLOCKED, card.getCardStatus());
        verify(cardBlockRequestRepository, never()).save(any());
    }

    @Test
    void approveRequest_shouldThrowWhenNotPending() {
        UUID personId = UUID.randomUUID();
        UUID cardId = UUID.randomUUID();
        Person person = createPerson(personId);
        Card card = createCard(cardId, person, CardStatus.ACTIVE);
        CardBlockRequest blockRequest = createBlockRequest(1L, card, person, BlockRequestStatus.APPROVED);
        when(cardBlockRequestRepository.findById(1L)).thenReturn(Optional.of(blockRequest));
        assertThrows(InvalidCardOperationException.class, () -> cardBlockRequestService.approveRequest(1L));
    }

    @Test
    void approveRequest_shouldThrowWhenNotFound() {
        when(cardBlockRequestRepository.findById(99L)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> cardBlockRequestService.approveRequest(99L));
    }

    @Test
    void rejectRequest_shouldRejectPendingRequest() {
        UUID personId = UUID.randomUUID();
        UUID cardId = UUID.randomUUID();
        Person person = createPerson(personId);
        Card card = createCard(cardId, person, CardStatus.ACTIVE);
        CardBlockRequest blockRequest = createBlockRequest(1L, card, person, BlockRequestStatus.PENDING);
        when(cardBlockRequestRepository.findById(1L)).thenReturn(Optional.of(blockRequest));
        when(cardMaskUtil.decryptAndMask("encrypted")).thenReturn("**** **** **** 7890");

        CardBlockRequestAdminResponse result = cardBlockRequestService.rejectRequest(1L);

        assertEquals(BlockRequestStatus.REJECTED, result.blockRequestStatus());
    }

    @Test
    void rejectRequest_shouldThrowWhenNotPending() {
        UUID personId = UUID.randomUUID();
        UUID cardId = UUID.randomUUID();
        Person person = createPerson(personId);
        Card card = createCard(cardId, person, CardStatus.ACTIVE);
        CardBlockRequest blockRequest = createBlockRequest(1L, card, person, BlockRequestStatus.REJECTED);
        when(cardBlockRequestRepository.findById(1L)).thenReturn(Optional.of(blockRequest));
        assertThrows(InvalidCardOperationException.class, () -> cardBlockRequestService.rejectRequest(1L));
    }

    @Test
    void rejectRequest_shouldThrowWhenNotFound() {
        when(cardBlockRequestRepository.findById(99L)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> cardBlockRequestService.rejectRequest(99L));
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
        c.setCardStatus(status);
        return c;
    }

    private CardBlockRequest createBlockRequest(Long id, Card card, Person person, BlockRequestStatus status) {
        CardBlockRequest r = new CardBlockRequest();
        r.setId(id);
        r.setCard(card);
        r.setPerson(person);
        r.setBlockRequestStatus(status);
        return r;
    }
}
