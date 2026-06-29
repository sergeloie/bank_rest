package com.example.bankcards.service;

import com.example.bankcards.dto.cardblockrequest.CardBlockRequestCreate;
import com.example.bankcards.dto.cardblockrequest.CardBlockRequestDto;
import com.example.bankcards.entity.BlockRequestStatus;
import com.example.bankcards.entity.Card;
import com.example.bankcards.entity.CardBlockRequest;
import com.example.bankcards.entity.CardStatus;
import com.example.bankcards.entity.Person;
import com.example.bankcards.exception.InvalidCardOperationException;
import com.example.bankcards.exception.ResourceNotFoundException;
import com.example.bankcards.mapper.CardBlockRequestMapper;
import com.example.bankcards.repository.CardBlockRequestRepository;
import com.example.bankcards.repository.CardRepository;
import com.example.bankcards.repository.PersonRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@RequiredArgsConstructor
@Service
public class CardBlockRequestService {
    private final CardBlockRequestRepository cardBlockRequestRepository;
    private final CardRepository cardRepository;
    private final PersonRepository personRepository;
    private final CardBlockRequestMapper cardBlockRequestMapper;

    @Transactional
    public CardBlockRequestDto createRequest(CardBlockRequestCreate request) {
        Card card = cardRepository.findById(request.cardId())
                .orElseThrow(() -> new ResourceNotFoundException("Card not found with id: " + request.cardId()));

        Person person = personRepository.findById(request.personId())
                .orElseThrow(() -> new ResourceNotFoundException("Person not found with id: " + request.personId()));

        if (!card.getPerson().getId().equals(person.getId())) {
            throw new InvalidCardOperationException("Card does not belong to this person");
        }

        if (card.getCardStatus() != CardStatus.ACTIVE) {
            throw new InvalidCardOperationException("Only active cards can be blocked");
        }

        CardBlockRequest blockRequest = cardBlockRequestMapper.toEntity(request);
        blockRequest.setCard(card);
        blockRequest.setPerson(person);
        blockRequest.setBlockRequestStatus(BlockRequestStatus.PENDING);

        return cardBlockRequestMapper.toDto(cardBlockRequestRepository.save(blockRequest));
    }

    public Page<CardBlockRequestDto> getPendingRequests(Pageable pageable) {
        return cardBlockRequestRepository.findByBlockRequestStatus(BlockRequestStatus.PENDING, pageable)
                .map(cardBlockRequestMapper::toDto);
    }

    @Transactional
    public CardBlockRequestDto approveRequest(Long requestId) {
        CardBlockRequest blockRequest = cardBlockRequestRepository.findById(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("Block request not found with id: " + requestId));

        if (blockRequest.getBlockRequestStatus() != BlockRequestStatus.PENDING) {
            throw new InvalidCardOperationException("Only pending requests can be approved");
        }

        blockRequest.setBlockRequestStatus(BlockRequestStatus.APPROVED);
        blockRequest.getCard().setCardStatus(CardStatus.BLOCKED);

        return cardBlockRequestMapper.toDto(cardBlockRequestRepository.save(blockRequest));
    }

    @Transactional
    public CardBlockRequestDto rejectRequest(Long requestId) {
        CardBlockRequest blockRequest = cardBlockRequestRepository.findById(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("Block request not found with id: " + requestId));

        if (blockRequest.getBlockRequestStatus() != BlockRequestStatus.PENDING) {
            throw new InvalidCardOperationException("Only pending requests can be rejected");
        }

        blockRequest.setBlockRequestStatus(BlockRequestStatus.REJECTED);

        return cardBlockRequestMapper.toDto(cardBlockRequestRepository.save(blockRequest));
    }
}
