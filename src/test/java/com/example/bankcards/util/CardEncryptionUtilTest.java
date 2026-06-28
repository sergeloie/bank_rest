package com.example.bankcards.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

public class CardEncryptionUtilTest {
    private static final String TEST_KEY = "0491a39ec1da88c8246b7a8e805bea21";
    private final CardEncryptionUtil cardEncryptionUtil = new CardEncryptionUtil(TEST_KEY);

    @Test
    void encrypt_thenDecrypt_returnsOriginal() {
        String original = "4111111111111111";

        String encrypted = cardEncryptionUtil.encrypt(original);
        String decrypted = cardEncryptionUtil.decrypt(encrypted);

        assertNotEquals(original, encrypted);
        assertEquals(original, decrypted);
    }

    @Test
    void encrypt_sameInputProducesSameOutput() {
        String input = "4111111111111111";

        String encrypted1 = cardEncryptionUtil.encrypt(input);
        String encrypted2 = cardEncryptionUtil.encrypt(input);

        assertEquals(encrypted1, encrypted2);
    }


    @Test
    void encrypt_differentInputs_produceDifferentOutputs() {
        String encrypted1 = cardEncryptionUtil.encrypt("4111111111111111");
        String encrypted2 = cardEncryptionUtil.encrypt("5500000000000004");

        assertNotEquals(encrypted1, encrypted2);
    }
}
