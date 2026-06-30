package com.example.bankcards.util;

import com.example.bankcards.config.CardProperties;
import com.example.bankcards.exception.CardNumberGenerationException;
import com.example.bankcards.repository.CardRepository;
import org.springframework.stereotype.Component;

import java.security.SecureRandom;

@Component
public class CardNumberGenerator {
    private static final int RANDOM_DIGIT_COUNT = 9;
    private final String bin;
    private final int retryLimit;
    private final SecureRandom secureRandom;
    private final CardRepository cardRepository;
    private final CardEncryptionUtil cardEncryptionUtil;

    public CardNumberGenerator(CardRepository cardRepository,
                               CardProperties cardProperties,
                               CardEncryptionUtil cardEncryptionUtil) {
        secureRandom = new SecureRandom();
        this.cardRepository = cardRepository;
        this.cardEncryptionUtil = cardEncryptionUtil;
        this.bin = cardProperties.getBin();
        this.retryLimit = cardProperties.getRetryLimit();
    }


    public String generate() {
        for (int attempt = 0; attempt < retryLimit; attempt++) {
            StringBuilder sb = new StringBuilder(bin);
            for (int i = 0; i < RANDOM_DIGIT_COUNT; i++) {
                sb.append(secureRandom.nextInt(10));
            }
            String partial = sb.toString();
            int checkDigit = luhnCheckDigit(partial);
            String candidate = partial + checkDigit;
            String cardHash = cardEncryptionUtil.hash(candidate);
            if (!cardRepository.existsByCardHash(cardHash)) {
                return candidate;
            }

        }
        throw new CardNumberGenerationException("Failed to generate unique card number after " + retryLimit + " attempts");
    }

    private int luhnCheckDigit(String partial) {
        int sum = 0;
        boolean doubleDigit = true;
        for (int i = partial.length() - 1; i >= 0; i--) {
            int digit = Character.getNumericValue(partial.charAt(i));
            if (doubleDigit) {
                digit *= 2;
                if (digit > 9) digit -= 9;
            }
            sum += digit;
            doubleDigit = !doubleDigit;
        }
        return (10 - (sum % 10)) % 10;
    }

}
