package com.example.bankcards.util;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;

class CardMaskUtilTest {

    private final CardMaskUtil cardMaskUtil = new CardMaskUtil(mock(CardEncryptionUtil.class));

    @ParameterizedTest
    @CsvSource({
            "4111111111111111, **** **** **** 1111",
            "5500000000000004, **** **** **** 0004",
            "1234567890123456, **** **** **** 3456"
    })
    void mask_showsOnlyLastFourDigits(String input, String expected) {
        assertEquals(expected, cardMaskUtil.mask(input));
    }

    @Test
    void mask_throwIfNumberIsNull() {
        Exception exception = assertThrows(RuntimeException.class, () -> cardMaskUtil.mask(null));
        assertEquals("Masked card number empty or invalid length", exception.getMessage());
    }

    @Test
    void mask_throwIfCardNumberInvalidLength() {
        Exception exception = assertThrows(RuntimeException.class, () -> cardMaskUtil.mask("123456789012345"));
        assertEquals("Masked card number empty or invalid length", exception.getMessage());
    }
}
