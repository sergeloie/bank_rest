package com.example.bankcards.scheduler;

import com.example.bankcards.entity.Card;
import com.example.bankcards.entity.CardStatus;
import com.example.bankcards.repository.CardRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class CardExpirationScheduler {

    private final CardRepository cardRepository;

    @Scheduled(cron = "0 0 2 * * *")
    @Transactional
    public void expireCards() {
        LocalDate today = LocalDate.now();
        List<Card> expiredCards = cardRepository.findByCardStatusAndExpirationDateBefore(
                CardStatus.ACTIVE, today);

        for (Card card : expiredCards) {
            card.setCardStatus(CardStatus.EXPIRED);
        }

        if (!expiredCards.isEmpty()) {
            cardRepository.saveAll(expiredCards);
            log.info("Expired {} cards with expiration date before {}", expiredCards.size(), today);
        }
    }
}
