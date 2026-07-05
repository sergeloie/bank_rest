package com.example.bankcards.service;

import com.example.bankcards.dto.transfer.TransferHistoryResponse;
import com.example.bankcards.entity.Card;
import com.example.bankcards.entity.Person;
import com.example.bankcards.entity.TransferHistory;
import com.example.bankcards.repository.TransferHistoryRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TransferHistoryServiceTest {

    @Mock
    private TransferHistoryRepository transferHistoryRepository;

    @InjectMocks
    private TransferHistoryService transferHistoryService;

    @Test
    void getTransfers_withPersonId_shouldReturnFilteredPage() {
        UUID personId = UUID.randomUUID();
        TransferHistory history = createHistory(personId);
        when(transferHistoryRepository.findByPerson_Id(eq(personId), any(PageRequest.class)))
                .thenReturn(new PageImpl<>(List.of(history), PageRequest.of(0, 10), 1));

        Page<TransferHistoryResponse> result = transferHistoryService.getTransfers(personId, PageRequest.of(0, 10));

        assertEquals(1, result.getContent().size());
        assertEquals(personId, result.getContent().get(0).personId());
    }

    @Test
    void getTransfers_withoutPersonId_shouldReturnAll() {
        TransferHistory history = createHistory(UUID.randomUUID());
        when(transferHistoryRepository.findAll(any(PageRequest.class)))
                .thenReturn(new PageImpl<>(List.of(history), PageRequest.of(0, 10), 1));

        Page<TransferHistoryResponse> result = transferHistoryService.getTransfers(null, PageRequest.of(0, 10));

        assertEquals(1, result.getContent().size());
    }

    @Test
    void getTransfers_shouldReturnEmptyPage() {
        UUID personId = UUID.randomUUID();
        when(transferHistoryRepository.findByPerson_Id(eq(personId), any(PageRequest.class)))
                .thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 10), 0));

        Page<TransferHistoryResponse> result = transferHistoryService.getTransfers(personId, PageRequest.of(0, 10));

        assertTrue(result.isEmpty());
    }

    private TransferHistory createHistory(UUID personId) {
        Person person = new Person();
        person.setId(personId);

        Person sender = new Person();
        sender.setId(UUID.randomUUID());

        Card fromCard = new Card();
        fromCard.setId(UUID.randomUUID());
        fromCard.setPerson(sender);

        Card toCard = new Card();
        toCard.setId(UUID.randomUUID());
        toCard.setPerson(person);

        TransferHistory history = new TransferHistory();
        history.setId(1L);
        history.setPerson(person);
        history.setFromCard(fromCard);
        history.setToCard(toCard);
        history.setAmount(BigDecimal.valueOf(100));
        return history;
    }
}
