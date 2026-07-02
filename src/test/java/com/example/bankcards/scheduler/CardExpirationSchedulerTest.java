package com.example.bankcards.scheduler;

import com.example.bankcards.entity.Card;
import com.example.bankcards.entity.CardStatus;
import com.example.bankcards.entity.Person;
import com.example.bankcards.entity.Role;
import com.example.bankcards.repository.CardRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CardExpirationSchedulerTest {

    @Mock
    private CardRepository cardRepository;

    @InjectMocks
    private CardExpirationScheduler scheduler;

    @Test
    void expireCards_whenExpiredCardsExist_setsStatusToExpired() {
        Person person = createPerson(1L);
        Card card1 = createCard(1L, person, CardStatus.ACTIVE, LocalDate.now().minusDays(1));
        Card card2 = createCard(2L, person, CardStatus.ACTIVE, LocalDate.now().minusDays(5));
        when(cardRepository.findByCardStatusAndExpirationDateBefore(eq(CardStatus.ACTIVE), any(LocalDate.class)))
                .thenReturn(List.of(card1, card2));

        scheduler.expireCards();

        assertEquals(CardStatus.EXPIRED, card1.getCardStatus());
        assertEquals(CardStatus.EXPIRED, card2.getCardStatus());
        verify(cardRepository).saveAll(List.of(card1, card2));
    }

    @Test
    void expireCards_whenNoCardsToExpire_doesNotCallSaveAll() {
        when(cardRepository.findByCardStatusAndExpirationDateBefore(eq(CardStatus.ACTIVE), any(LocalDate.class)))
                .thenReturn(List.of());

        scheduler.expireCards();

        verify(cardRepository, never()).saveAll(any());
    }

    @Test
    void expireCards_callsRepositoryWithCorrectParameters() {
        when(cardRepository.findByCardStatusAndExpirationDateBefore(eq(CardStatus.ACTIVE), any(LocalDate.class)))
                .thenReturn(List.of());

        scheduler.expireCards();

        verify(cardRepository).findByCardStatusAndExpirationDateBefore(eq(CardStatus.ACTIVE), any(LocalDate.class));
    }

    private Person createPerson(Long id) {
        Person p = new Person();
        p.setId(id);
        p.setName("Alice");
        p.setPassword("pass");
        p.setRole(Role.USER);
        return p;
    }

    private Card createCard(Long id, Person person, CardStatus status, LocalDate expirationDate) {
        Card c = new Card();
        c.setId(id);
        c.setPerson(person);
        c.setEncryptedNumber("encrypted");
        c.setCardHash("hash");
        c.setExpirationDate(expirationDate);
        c.setCardStatus(status);
        c.setBalance(BigDecimal.valueOf(100));
        return c;
    }
}
