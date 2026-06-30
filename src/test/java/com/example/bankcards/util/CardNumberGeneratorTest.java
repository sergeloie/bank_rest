package com.example.bankcards.util;

import com.example.bankcards.config.CardProperties;
import com.example.bankcards.exception.CardNumberGenerationException;
import com.example.bankcards.repository.CardRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class CardNumberGeneratorTest {

    private final int maxRetry = 1989;
    private final String bin = "155155";
    private final CardRepository cardRepository = mock(CardRepository.class);
    private final CardProperties cardProperties = mock(CardProperties.class);
    private final CardEncryptionUtil cardEncryptionUtil = mock(CardEncryptionUtil.class);
    private CardNumberGenerator cardNumberGenerator;


    @BeforeEach
    void prepare() {
        when(cardProperties.getBin()).thenReturn(bin);
        when(cardProperties.getRetryLimit()).thenReturn(maxRetry);
        cardNumberGenerator = new CardNumberGenerator(cardRepository, cardProperties, cardEncryptionUtil);
    }


    @Test
    void generateTest() {
        when(cardRepository.existsByEncryptedNumber(any())).thenReturn(false);
        String card1 = cardNumberGenerator.generate();
        String card2 = cardNumberGenerator.generate();
        assertNotEquals(card1, card2);
        assertEquals(bin, card1.substring(0,6));
        assertEquals(bin, card2.substring(0,6));
        assertTrue(isValidLuhn(card1));
        assertTrue(isValidLuhn(card2));
    }

    @Test
    void generateTestThrowException() {
        when(cardRepository.existsByEncryptedNumber(any())).thenReturn(true);
        Exception exception = assertThrows(CardNumberGenerationException.class, cardNumberGenerator::generate);
        assertEquals("Failed to generate unique card number after " + maxRetry + " attempts", exception.getMessage());
    }

    private boolean isValidLuhn(String cardNumber) {
        int sum = 0;
        boolean doubleDigit = false;
        for (int i = cardNumber.length() - 1; i >= 0; i--) {
            int digit = Character.getNumericValue(cardNumber.charAt(i));
            if (doubleDigit) {
                digit *= 2;
                if (digit > 9) digit -= 9;
            }
            sum += digit;
            doubleDigit = !doubleDigit;
        }
        return sum % 10 == 0;
    }
}
