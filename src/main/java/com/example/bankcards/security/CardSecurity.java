package com.example.bankcards.security;

import com.example.bankcards.entity.Person;
import com.example.bankcards.repository.CardRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component("cardSecurity")
@RequiredArgsConstructor
public class CardSecurity {

    private final CardRepository cardRepository;

    public boolean isOwner(UUID cardId, Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof Person person)) {
            return false;
        }
        return cardRepository.existsByIdAndPerson_Id(cardId, person.getId());
    }

    public boolean isOwnerByPersonId(UUID personId, Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof Person person)) {
            return false;
        }
        return person.getId().equals(personId);
    }
}