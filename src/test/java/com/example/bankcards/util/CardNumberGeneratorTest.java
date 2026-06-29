package com.example.bankcards.util;

import com.example.bankcards.config.CardProperties;
import com.example.bankcards.exception.CardNumberGenerationException;
import com.example.bankcards.repository.CardRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class CardNumberGeneratorTest {

    private final int MAX_RETRY = 1989;
    private final String BIN = "155155";
    private final CardRepository cardRepository = mock(CardRepository.class);
    private final CardProperties cardProperties = mock(CardProperties.class);
    private final CardEncryptionUtil cardEncryptionUtil = mock(CardEncryptionUtil.class);
    private CardNumberGenerator cardNumberGenerator;


    @BeforeEach
    void prepare() {
        when(cardProperties.getBin()).thenReturn(BIN);
        when(cardProperties.getRetryLimit()).thenReturn(MAX_RETRY);
        cardNumberGenerator = new CardNumberGenerator(cardRepository, cardProperties, cardEncryptionUtil);
    }


    @Test
    public void generateTest() {
        when(cardRepository.existsByEncryptedNumber(any())).thenReturn(false);
        String card1 = cardNumberGenerator.generate();
        String card2 = cardNumberGenerator.generate();
        assertNotEquals(card1, card2);
        assertEquals(BIN, card1.substring(0,6));
        assertEquals(BIN, card2.substring(0,6));
        assertTrue(cardNumberGenerator.isValidLuhn(card1));
        assertTrue(cardNumberGenerator.isValidLuhn(card2));
    }

    @Test
    public void isValidLuhnTest() {
        assertTrue(cardNumberGenerator.isValidLuhn("4111111111111111"));
        assertTrue(cardNumberGenerator.isValidLuhn("4627100101654724"));
        assertTrue(cardNumberGenerator.isValidLuhn("5204240438720059"));
        assertTrue(cardNumberGenerator.isValidLuhn("5204240438720067"));
        assertTrue(cardNumberGenerator.isValidLuhn("2201382000000021"));
        assertTrue(cardNumberGenerator.isValidLuhn("2204290100000006"));
    }

    @Test
    public void luhnCheckDigitTest() {
        assertEquals(1, cardNumberGenerator.luhnCheckDigit("220138200000002"));
        assertEquals(1, cardNumberGenerator.luhnCheckDigit("411111111111111"));
        assertEquals(7, cardNumberGenerator.luhnCheckDigit("520424043872006"));
    }


    @Test
    void generateTestThrowException() {
        when(cardRepository.existsByEncryptedNumber(any())).thenReturn(true);
        Exception exception = assertThrows(CardNumberGenerationException.class, cardNumberGenerator::generate);
        assertEquals("Failed to generate unique card number after " + MAX_RETRY + " attempts", exception.getMessage());
    }
}
