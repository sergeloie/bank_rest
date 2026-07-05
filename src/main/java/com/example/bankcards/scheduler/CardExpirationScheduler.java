package com.example.bankcards.scheduler;

import com.example.bankcards.entity.Card;
import com.example.bankcards.entity.CardStatus;
import com.example.bankcards.repository.CardRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Slf4j
@Component
@RequiredArgsConstructor
public class CardExpirationScheduler {

    private final CardRepository cardRepository;
    private static final int BATCH_SIZE = 100;

    @Scheduled(cron = "0 0 2 * * *")
    @Transactional
    public void expireCards() {
        LocalDate today = LocalDate.now();
        int totalExpired = 0;
        int page = 0;

        Page<Card> expiredCardsPage;
        do {
            expiredCardsPage = cardRepository.findByCardStatusAndExpirationDateBefore(
                    CardStatus.ACTIVE, today, PageRequest.of(page, BATCH_SIZE));

            if (!expiredCardsPage.getContent().isEmpty()) {
                for (Card card : expiredCardsPage.getContent()) {
                    card.setCardStatus(CardStatus.EXPIRED);
                }
                cardRepository.saveAll(expiredCardsPage.getContent());
                totalExpired += expiredCardsPage.getContent().size();
                page++;
            }
        } while (expiredCardsPage.hasNext());

        if (totalExpired > 0) {
            log.info("Expired {} cards with expiration date before {}", totalExpired, today);
        }
    }
}