package com.example.bankcards.security;

import com.example.bankcards.entity.Card;
import com.example.bankcards.entity.Person;
import com.example.bankcards.repository.CardRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

@Component("cardSecurity")
@RequiredArgsConstructor
public class CardSecurity {

    private final CardRepository cardRepository;

    public boolean isOwner(Long cardId, Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof Person person)) {
            return false;
        }
        return cardRepository.findById(cardId)
                .map(card -> card.getPerson().getId().equals(person.getId()))
                .orElse(false);
    }

    public boolean isOwnerByPersonId(Long personId, Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof Person person)) {
            return false;
        }
        return person.getId().equals(personId);
    }
}
